package com.halilibrahim.halilibrahimmmo.backup;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;
import com.halilibrahim.halilibrahimmmo.database.DatabaseType;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ZIP-archive implementation of {@link BackupService}.
 *
 * <h2>Three decisions worth knowing before changing anything here</h2>
 *
 * <p><strong>The database is snapshotted, not copied.</strong> A SQLite file
 * copied while the server is running is a file with a write halfway through it;
 * restoring one is how a backup turns into a second outage. SQLite has an answer
 * - {@code VACUUM INTO}, which writes a consistent copy of the whole database
 * from inside a transaction - so that is what runs, on the database's own thread
 * pool, and the <em>snapshot</em> is what goes into the archive. For MySQL,
 * MariaDB and PostgreSQL the data is not the server's to copy at all, and the
 * archive says so in a plain-text note rather than silently omitting it.</p>
 *
 * <p><strong>Worlds are flushed first.</strong> {@code World#save()} has to run
 * on the main thread, so it is hopped there and waited for before a single byte
 * is read. Without it the archive holds whatever was last autosaved, which on a
 * default server can be five minutes behind - and five minutes is exactly the
 * window a crash backup exists to close.</p>
 *
 * <p><strong>It runs on its own single thread, never on a scheduler.</strong>
 * Paper's schedulers are for gameplay work; zipping a world folder is neither
 * fast nor bounded, and putting it on a region thread would stall the region it
 * belongs to. One daemon thread, one backup at a time, refused rather than
 * queued.</p>
 */
public final class ZipBackupService implements BackupService {

    private static final String FILE_NAME = "yedek.yml";
    /** Written on enable, deleted on disable. Its presence at enable means the last run crashed. */
    private static final String MARKER_NAME = ".calisiyor";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT);
    /** How long the world flush is waited for before the backup goes ahead without it. */
    private static final long FLUSH_TIMEOUT_SECONDS = 30L;
    /**
     * How long a startup or crash backup waits before it runs.
     *
     * <p>{@code onEnable} is early: the plugin is up but the server has not
     * loaded its worlds yet, so {@code getWorlds()} is still empty and a backup
     * taken there archives the plugin folder and nothing else. Twenty seconds is
     * comfortably past a normal world load and still inside the first minute.</p>
     */
    private static final long STARTUP_DELAY_SECONDS = 20L;
    /**
     * Directories inside a world folder that hold player state rather than
     * terrain.
     *
     * <p>Both spellings of the player folder are listed on purpose: the classic
     * layout calls it {@code playerdata} and Minecraft 26's single-folder layout
     * calls it {@code players}. Whichever exists is taken, and a name that does
     * not exist costs one {@code isDirectory} check.</p>
     */
    private static final List<String> PLAYER_DATA_FOLDERS =
            List.of("playerdata", "players", "stats", "advancements");
    /**
     * How far above a dimension folder the level folder is looked for.
     *
     * <p>Three is one more than any layout the game has shipped
     * ({@code world/dimensions/minecraft/the_nether} is two), and a bound is what
     * stops a missing {@code level.dat} from walking this up to the filesystem
     * root.</p>
     */
    private static final int MAXIMUM_LEVEL_ROOT_DEPTH = 3;

    private final JavaPlugin plugin;
    private final DatabaseService database;
    private final DatabaseType databaseType;
    private final PluginLogger logger;
    private final AtomicReference<BackupSettings> settings = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<Instant> lastBackupAt = new AtomicReference<>();
    private volatile ScheduledExecutorService worker;
    private volatile boolean previousRunCrashed;

    public ZipBackupService(JavaPlugin plugin, DatabaseService database, DatabaseType databaseType, PluginLogger logger) {
        this.plugin = plugin;
        this.database = database;
        this.databaseType = databaseType;
        this.logger = logger;
    }

    @Override
    public void enable() {
        reload();
        previousRunCrashed = markerFile().exists();
        writeMarker();
        BackupSettings current = settings();
        if (!current.enabled()) {
            logger.info("Yedekleme kapali (yedek.yml -> etkin: false). Hicbir yedek alinmayacak.");
            return;
        }
        worker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "HalilIbrahimMMO-Yedek");
            thread.setDaemon(true);
            return thread;
        });
        // Delayed rather than immediate, and this is load-bearing: enable() runs
        // before the server has finished loading its worlds, so a backup taken
        // here saw getWorlds() return an empty list and quietly archived the
        // plugin folder and nothing else - missing exactly the player data a
        // crash backup exists to save. Waiting is the whole fix; the window it
        // opens (a second crash inside the first few seconds of a boot) is not a
        // window anything could have covered anyway.
        if (previousRunCrashed && current.onUncleanShutdown()) {
            logger.warn("Sunucu en son DUZGUN KAPANMAMIS. Dunyalar yuklenince guvenlik yedegi alinacak.");
            worker.schedule(() -> backupNow("cokme"), STARTUP_DELAY_SECONDS, TimeUnit.SECONDS);
        } else if (current.onStartup()) {
            worker.schedule(() -> backupNow("acilis"), STARTUP_DELAY_SECONDS, TimeUnit.SECONDS);
        }
        if (current.scheduled()) {
            long minutes = Math.max(1L, current.interval().toMinutes());
            worker.scheduleWithFixedDelay(() -> backupNow("zamanli"), minutes, minutes, TimeUnit.MINUTES);
            logger.info("Yedekleme etkin: her " + minutes + " dakikada bir, en fazla " + current.keep() + " arsiv saklanir.");
        }
    }

    @Override
    public void disable() {
        ScheduledExecutorService current = worker;
        worker = null;
        if (current != null) {
            current.shutdownNow();
        }
        // Deleted last, and only here: this file being gone is the entire
        // definition of "shut down cleanly", so anything that could still fail
        // has to have failed before it.
        deleteMarker();
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            // A malformed backup config must not stop a server booting - the
            // safe defaults still protect the data, which is the whole point.
            logger.error("yedek.yml okunamadi, guvenli varsayilanlar kullaniliyor: " + exception.getMessage(), exception);
            settings.set(BackupSettings.safeDefaults());
        }
    }

    private BackupSettings parse(YamlConfiguration yaml) {
        long minutes = yaml.getLong("aralik-dakika", 360L);
        if (minutes < 0L) {
            throw new IllegalArgumentException("aralik-dakika negatif olamaz.");
        }
        return new BackupSettings(
                yaml.getBoolean("etkin", true),
                yaml.getString("klasor", "hmmo-yedekler"),
                Duration.ofMinutes(minutes),
                yaml.getInt("saklanan-adet", 8),
                yaml.getBoolean("aciliste-al", false),
                yaml.getBoolean("cokmeden-sonra-al", true),
                yaml.getBoolean("icerik.eklenti-verisi", true),
                yaml.getBoolean("icerik.oyuncu-verisi", true),
                yaml.getBoolean("icerik.dunyalar", false),
                Math.max(1L, yaml.getLong("maksimum-boyut-mb", 2048L)) * 1024L * 1024L
        );
    }

    @Override
    public BackupSettings settings() {
        BackupSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("BackupService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public Optional<Instant> lastBackupAt() {
        return Optional.ofNullable(lastBackupAt.get());
    }

    @Override
    public boolean previousRunCrashed() {
        return previousRunCrashed;
    }

    @Override
    public CompletableFuture<BackupResult> backupNow(String label) {
        BackupSettings current = settings();
        if (!current.enabled()) {
            return CompletableFuture.completedFuture(BackupResult.of(BackupResult.Reason.DISABLED));
        }
        ScheduledExecutorService executor = worker;
        if (executor == null || executor.isShutdown()) {
            return CompletableFuture.completedFuture(BackupResult.of(BackupResult.Reason.DISABLED));
        }
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(BackupResult.of(BackupResult.Reason.ALREADY_RUNNING));
        }
        CompletableFuture<BackupResult> answer = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                BackupResult result = run(current, label);
                if (result.successful()) {
                    lastBackupAt.set(Instant.now());
                }
                answer.complete(result);
            } catch (RuntimeException exception) {
                logger.error("Yedekleme beklenmedik bir hatayla durdu: " + exception.getMessage(), exception);
                answer.complete(BackupResult.of(BackupResult.Reason.WRITE_FAILED));
            } finally {
                running.set(false);
            }
        });
        return answer;
    }

    private BackupResult run(BackupSettings current, String label) {
        if (!current.includePluginData() && !current.includePlayerData() && !current.includeWorlds()) {
            logger.warn("Yedekleme istendi ama yedek.yml'de hicbir icerik secili degil.");
            return BackupResult.of(BackupResult.Reason.NOTHING_TO_DO);
        }
        Path destination = plugin.getServer().getWorldContainer().toPath().resolve(current.directory());
        Path snapshot = null;
        try {
            Files.createDirectories(destination);
        } catch (IOException exception) {
            logger.error("Yedek klasoru olusturulamadi (" + destination + "): " + exception.getMessage(), exception);
            return BackupResult.of(BackupResult.Reason.WRITE_FAILED);
        }
        String safeLabel = label.replaceAll("[^a-zA-Z0-9-]", "");
        Path archive = destination.resolve("hmmo-" + LocalDateTime.now().format(STAMP)
                + (safeLabel.isEmpty() ? "" : "-" + safeLabel) + ".zip");

        flushWorlds();
        int entries = 0;
        try (OutputStream out = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            if (current.includePluginData()) {
                snapshot = snapshotDatabase();
                entries += addTree(zip, plugin.getDataFolder().toPath(), "eklenti", this::isArchivable);
                if (snapshot != null) {
                    entries += addFile(zip, snapshot, "eklenti/veritabani-anlik.db");
                } else if (databaseType != DatabaseType.SQLITE) {
                    entries += addNote(zip);
                }
            }
            entries += addWorldContent(zip, current);
        } catch (IOException exception) {
            logger.error("Yedek yazilamadi (" + archive.getFileName() + "): " + exception.getMessage(), exception);
            deleteQuietly(archive);
            return BackupResult.of(BackupResult.Reason.WRITE_FAILED);
        } finally {
            deleteQuietly(snapshot);
        }

        long size = sizeOf(archive);
        if (size > current.maximumArchiveBytes()) {
            logger.warn("Yedek " + (size / 1024L / 1024L) + " MB oldu ve sinirin ("
                    + (current.maximumArchiveBytes() / 1024L / 1024L) + " MB) ustunde. Silindi. "
                    + "yedek.yml -> icerik.dunyalar ayarini kapatmayi ya da siniri yukseltmeyi dusunun.");
            deleteQuietly(archive);
            return BackupResult.of(BackupResult.Reason.TOO_LARGE);
        }
        prune(destination, current.keep());
        logger.info("Yedek alindi: " + archive.getFileName() + " (" + entries + " dosya, "
                + Math.max(1L, size / 1024L / 1024L) + " MB)");
        return new BackupResult(BackupResult.Reason.OK, archive.getFileName().toString(), size, entries);
    }

    /**
     * Which world content goes in, by what the settings asked for.
     *
     * <p>Player data is deliberately separable from terrain. What a crash
     * actually loses is somebody's inventory, and that is a few megabytes;
     * copying the terrain as well is the same operation an order of magnitude
     * more expensive, and on most servers the terrain is the part a host's own
     * snapshot already covers.</p>
     *
     * <p><strong>A world folder is not a level folder, and mistaking one for the
     * other is how the first working version of this shipped an archive with no
     * inventories in it.</strong> {@code World#getWorldFolder()} answers with the
     * <em>dimension</em> directory - the one holding {@code region} and
     * {@code data} - while {@code level.dat} and the player files live at the
     * level root above it, shared by all three dimensions. So the pass runs
     * twice: once per world for the dimension's own state, and once per distinct
     * level root for the part a player would actually miss.</p>
     */
    private int addWorldContent(ZipOutputStream zip, BackupSettings current) throws IOException {
        if (!current.includeWorlds() && !current.includePlayerData()) {
            return 0;
        }
        List<World> worlds = plugin.getServer().getWorlds();
        if (worlds.isEmpty()) {
            // The one failure mode that looks like success: an archive full of
            // configuration and none of the data it was taken for.
            logger.warn("Yedek alinirken hicbir dunya yuklu degildi - arsivde OYUNCU VERISI YOK. "
                    + "Bu bir hatadir; konsolu bildirin.");
            return 0;
        }
        int entries = 0;
        java.util.Set<Path> levelRoots = new java.util.LinkedHashSet<>();
        for (World world : worlds) {
            Path folder = world.getWorldFolder().toPath();
            if (!Files.isDirectory(folder)) {
                logger.warn("Dunya klasoru bulunamadi, atlandi: " + world.getName() + " -> " + folder);
                continue;
            }
            levelRoots.add(levelRootOf(folder));
            if (current.includeWorlds()) {
                entries += addTree(zip, folder, "dunya/" + world.getName(), path -> true);
                continue;
            }
            // The dimension's own state: game rules, world border, raids,
            // scheduled events. Small, and without it a restored level is a
            // different world than the one that crashed.
            Path data = folder.resolve("data");
            if (Files.isDirectory(data)) {
                entries += addTree(zip, data, "dunya/" + world.getName() + "/data", path -> true);
            }
        }
        if (current.includePlayerData()) {
            for (Path root : levelRoots) {
                entries += addLevelRoot(zip, root);
            }
        }
        if (entries == 0) {
            // Same class of failure as an empty world list, one step further in:
            // the worlds are there but nothing under them matched. Said out loud
            // rather than left as a smaller number in the summary line, because
            // the summary looks perfectly healthy either way.
            logger.warn("Yedege hicbir dunya dosyasi eklenemedi (" + worlds.size()
                    + " dunya tarandi). Arsivde OYUNCU VERISI YOK.");
        } else {
            logger.info("Yedege " + entries + " dunya dosyasi eklendi (" + worlds.size()
                    + " dunya, " + levelRoots.size() + " seviye koku).");
        }
        return entries;
    }

    /**
     * {@code level.dat} and every player-bearing folder at one level root.
     *
     * <p>{@code level.dat} carries the seed, the spawn and the game rules - a few
     * kilobytes that turn a pile of player files back into a server.</p>
     */
    private int addLevelRoot(ZipOutputStream zip, Path root) throws IOException {
        String label = "seviye/" + root.getFileName();
        int entries = 0;
        Path levelData = root.resolve("level.dat");
        if (Files.isRegularFile(levelData)) {
            entries += addFile(zip, levelData, label + "/level.dat");
        }
        for (String name : PLAYER_DATA_FOLDERS) {
            Path sub = root.resolve(name);
            if (Files.isDirectory(sub)) {
                entries += addTree(zip, sub, label + "/" + name, path -> true);
            }
        }
        return entries;
    }

    /**
     * Walks up from a dimension folder to the level folder that owns it - the
     * nearest ancestor holding a {@code level.dat}.
     *
     * <p>Bounded rather than unbounded: a missing {@code level.dat} must not send
     * this climbing out of the server directory and into the filesystem root. If
     * nothing is found the dimension folder itself is answered, which archives
     * less rather than archiving something it should not.</p>
     */
    private static Path levelRootOf(Path dimensionFolder) {
        Path candidate = dimensionFolder;
        for (int step = 0; step <= MAXIMUM_LEVEL_ROOT_DEPTH && candidate != null; step++) {
            if (Files.isRegularFile(candidate.resolve("level.dat"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        return dimensionFolder;
    }

    /**
     * Asks every world to write itself out, and waits.
     *
     * <p>Hopped onto the global region scheduler because {@code World#save} is
     * Bukkit API. Timed out rather than waited on forever: a backup that never
     * finishes because one region is busy is worse than a backup that is a few
     * seconds stale, and the timeout is logged so the operator knows which one
     * they got.</p>
     */
    private void flushWorlds() {
        CompletableFuture<Void> flushed = new CompletableFuture<>();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            try {
                plugin.getServer().getWorlds().forEach(World::save);
            } catch (RuntimeException exception) {
                logger.warn("Dunyalar kaydedilirken hata: " + exception.getMessage());
            } finally {
                flushed.complete(null);
            }
        });
        try {
            flushed.get(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            logger.warn("Dunyalarin kaydedilmesi " + FLUSH_TIMEOUT_SECONDS
                    + " saniyede bitmedi; yedek son otomatik kayitla aliniyor.");
        }
    }

    /**
     * A consistent copy of the SQLite database, or {@code null} when there is
     * nothing local to copy.
     *
     * <p>{@code VACUUM INTO} runs inside a transaction and writes a whole,
     * readable database file - which a byte-for-byte copy of a live one is not.
     * Executed through the database service's own pool, so it queues behind
     * whatever else is running rather than fighting it.</p>
     */
    private Path snapshotDatabase() {
        if (databaseType != DatabaseType.SQLITE) {
            return null;
        }
        try {
            Path target = Files.createTempFile("hmmo-veritabani", ".db");
            // VACUUM INTO refuses to overwrite, so the placeholder the temp-file
            // call just created has to go first.
            Files.deleteIfExists(target);
            String escaped = target.toAbsolutePath().toString().replace("'", "''");
            database.executeVoid(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("VACUUM INTO '" + escaped + "'");
                }
            }).get(FLUSH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return Files.isRegularFile(target) ? target : null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception exception) {
            logger.warn("Veritabani anlik goruntusu alinamadi, yedege canli dosya konulmadi: " + exception.getMessage());
            return null;
        }
    }

    /**
     * What is left out of the plugin folder walk.
     *
     * <p>The live database, because the consistent snapshot goes in instead; and
     * the crash marker, because restoring an archive that contains it would make
     * the very next boot believe it had crashed and take another backup for no
     * reason.</p>
     */
    private boolean isArchivable(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals(MARKER_NAME)) {
            return false;
        }
        return !(name.endsWith(".db") || name.endsWith(".db-wal") || name.endsWith(".db-shm"));
    }

    private int addTree(ZipOutputStream zip, Path root, String prefix, java.util.function.Predicate<Path> accept)
            throws IOException {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        int[] written = {0};
        IOException[] failure = new IOException[1];
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (!accept.test(file)) {
                    return FileVisitResult.CONTINUE;
                }
                try {
                    written[0] += addFile(zip, file, prefix + "/" + root.relativize(file).toString().replace('\\', '/'));
                } catch (IOException exception) {
                    failure[0] = exception;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) {
                // A single unreadable file - a lock, a socket, something the OS
                // is holding - must not lose the whole archive.
                logger.warn("Yedege eklenemedi: " + file + " (" + exception.getMessage() + ")");
                return FileVisitResult.CONTINUE;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        return written[0];
    }

    private int addFile(ZipOutputStream zip, Path file, String entryName) throws IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zip);
        zip.closeEntry();
        return 1;
    }

    /** A plain-text note for an external database, so an operator opening the archive is not misled. */
    private int addNote(ZipOutputStream zip) throws IOException {
        zip.putNextEntry(new ZipEntry("eklenti/VERITABANI-HARICI.txt"));
        zip.write(("Bu sunucu " + databaseType.name() + " kullaniyor.\r\n"
                + "Veritabani sunucunun disinda oldugu icin bu yedege DAHIL EDILMEDI.\r\n"
                + "Oyuncu ilerlemesini korumak icin veritabaninin kendi yedegini ayrica alin.\r\n")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
        return 1;
    }

    /** Deletes the oldest archives until {@code keep} remain. Only ever touches files this class named. */
    private void prune(Path directory, int keep) {
        try (var stream = Files.list(directory)) {
            List<Path> archives = new ArrayList<>(stream
                    .filter(path -> path.getFileName().toString().startsWith("hmmo-"))
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .toList());
            if (archives.size() <= keep) {
                return;
            }
            archives.sort(Comparator.comparing(Path::getFileName).reversed());
            archives.subList(keep, archives.size()).forEach(this::deleteQuietly);
        } catch (IOException exception) {
            logger.warn("Eski yedekler temizlenemedi: " + exception.getMessage());
        }
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            logger.warn("Silinemedi: " + path + " (" + exception.getMessage() + ")");
        }
    }

    private File markerFile() {
        return new File(plugin.getDataFolder(), MARKER_NAME);
    }

    private void writeMarker() {
        File marker = markerFile();
        try {
            if (!marker.getParentFile().exists() && !marker.getParentFile().mkdirs()) {
                throw new IOException("veri klasoru olusturulamadi");
            }
            Files.writeString(marker.toPath(), Instant.now().toString());
        } catch (IOException exception) {
            // Not fatal: without the marker the crash check simply never fires,
            // and the scheduled backups still run.
            logger.warn("Calisma isareti yazilamadi, cokme tespiti bu oturumda calismayacak: " + exception.getMessage());
        }
    }

    private void deleteMarker() {
        try {
            Files.deleteIfExists(markerFile().toPath());
        } catch (IOException exception) {
            logger.warn("Calisma isareti silinemedi; bir sonraki acilis cokme sanacak: " + exception.getMessage());
        }
    }
}
