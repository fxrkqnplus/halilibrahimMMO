package com.halilibrahim.halilibrahimmmo.resourcepack;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Paper implementation of {@link ResourcePackService}.
 *
 * <p>Timeline for one join, all of it on the player's own region scheduler:</p>
 * <ol>
 *   <li>{@code gecikme-tik} ticks after join, offer the pack.</li>
 *   <li>Every second afterwards, check whether the download has stalled; if it
 *       has, claim the decision and kick.</li>
 *   <li>A terminal status from the client claims the decision first and either
 *       marks the player applied or kicks them with a specific reason.</li>
 * </ol>
 */
public final class PaperResourcePackService implements ResourcePackService {

    private static final String FILE_NAME = "kaynak-paketi.yml";
    private static final String SECTION = "kaynak-paketi";
    private static final long WATCHDOG_PERIOD_TICKS = 20L;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final PluginLogger logger;
    private final Map<UUID, ResourcePackState> states = new ConcurrentHashMap<>();
    private final AtomicReference<ResourcePackSettings> settings = new AtomicReference<>();

    public PaperResourcePackService(JavaPlugin plugin, MessageService messages, PluginLogger logger) {
        this.plugin = plugin;
        this.messages = messages;
        this.logger = logger;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        states.clear();
    }

    /**
     * Unlike every other config in this plugin, a broken file here does
     * <strong>not</strong> raise {@code LifecycleException}. A pack that cannot
     * be served must degrade to "no pack", never to "no players" - see
     * {@link ResourcePackSettings} for the full argument.
     */
    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        ResourcePackSettings parsed;
        try {
            parsed = parse(YamlConfiguration.loadConfiguration(file));
        } catch (RuntimeException exception) {
            logger.error("kaynak-paketi.yml okunamadi, kaynak paketi kapali kabul ediliyor.", exception);
            settings.set(ResourcePackSettings.disabled());
            return;
        }

        String unusable = parsed.unusableReason();
        if (unusable != null) {
            logger.error("Kaynak paketi acik ama kullanilamaz (" + unusable
                    + "). Hicbir oyuncuya paket gonderilmeyecek ve kimse atilmayacak."
                    + " kaynak-paketi.yml dosyasini duzeltip /haliladmin -> Yeniden Yukle deyin.");
            settings.set(new ResourcePackSettings(
                    false, parsed.url(), parsed.sha1(), parsed.required(), parsed.timeoutSeconds(),
                    parsed.delayTicks(), parsed.bypassPermission(), false, false));
            return;
        }

        settings.set(parsed);
        if (parsed.enabled()) {
            logger.info("Kaynak paketi etkin: " + parsed.host() + " ("
                    + PackChecksum.abbreviate(parsed.sha1(), 8) + "), zorunlu=" + parsed.required() + ".");
        }
    }

    private ResourcePackSettings parse(YamlConfiguration yaml) {
        ConfigurationSection section = yaml.getConfigurationSection(SECTION);
        if (section == null) {
            throw new IllegalArgumentException("kaynak-paketi.yml is missing the '" + SECTION + "' section.");
        }
        return new ResourcePackSettings(
                section.getBoolean("etkin", false),
                section.getString("adres", ""),
                section.getString("sha1", ""),
                section.getBoolean("zorunlu", true),
                section.getLong("zaman-asimi-saniye", 60L),
                section.getLong("gecikme-tik", 20L),
                section.getString("bypass-izni", ResourcePackSettings.DEFAULT_BYPASS_PERMISSION),
                section.getBoolean("arayuz-arkaplani", false),
                section.getBoolean("eylem-cubugu-barlari", true)
        );
    }

    @Override
    public ResourcePackSettings settings() {
        ResourcePackSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("ResourcePackService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public void begin(Player player) {
        ResourcePackSettings current = settings();
        if (!current.usable()) {
            return;
        }

        boolean exempt = player.hasPermission(current.bypassPermission());
        ResourcePackState state = exempt ? ResourcePackState.exempt() : ResourcePackState.pending();
        states.put(player.getUniqueId(), state);

        byte[] digest = PackChecksum.parse(current.sha1());
        // Derived from the URL so the same pack always carries the same id
        // across restarts; a random id would make removeResourcePack(...)
        // impossible to aim and would confuse a client that already has it.
        UUID packId = UUID.nameUUIDFromBytes(current.url().getBytes(StandardCharsets.UTF_8));
        Component prompt = messages.render(MessageKey.RESOURCEPACK_PROMPT);
        // An exempt player is offered the pack but never forced: staff should
        // see what players see without giving up the permission that keeps them
        // from locking themselves out of their own server (§18.6).
        boolean force = current.required() && !exempt;

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) {
                return;
            }
            player.setResourcePack(packId, current.url(), digest, prompt, force);
            if (!exempt) {
                armWatchdog(player, state, current);
            }
        }, null, Math.max(1L, current.delayTicks()));
    }

    /**
     * Fires once a second and only acts when the download has made no progress
     * for the whole timeout. A one-shot {@code runDelayed} would kill a large
     * pack mid-download on a slow line; this only kills a download that has
     * actually stopped.
     */
    private void armWatchdog(Player player, ResourcePackState state, ResourcePackSettings current) {
        player.getScheduler().runAtFixedRate(plugin, (ScheduledTask task) -> {
            if (state.isResolved() || !player.isOnline()) {
                task.cancel();
                return;
            }
            if (!state.isStalled(current.timeoutMillis())) {
                return;
            }
            task.cancel();
            if (state.resolve()) {
                fail(player, MessageKey.RESOURCEPACK_KICK_TIMEOUT, current,
                        "paket indirme " + current.timeoutSeconds() + " saniye boyunca ilerlemedi");
            }
        }, null, WATCHDOG_PERIOD_TICKS, WATCHDOG_PERIOD_TICKS);
    }

    @Override
    public void handleStatus(Player player, PlayerResourcePackStatusEvent.Status status) {
        ResourcePackState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        state.lastReported(status);
        ResourcePackSettings current = settings();

        switch (status) {
            // Progress, not a verdict: reset the watchdog's patience and wait.
            case ACCEPTED, DOWNLOADED -> state.touch();

            case SUCCESSFULLY_LOADED -> {
                if (state.resolve()) {
                    state.markApplied();
                    player.getScheduler().run(plugin,
                            task -> messages.send(player, MessageKey.RESOURCEPACK_APPLIED), null);
                }
            }
            case DECLINED -> terminal(player, state, current, MessageKey.RESOURCEPACK_KICK_DECLINED, "paketi reddetti");
            case FAILED_DOWNLOAD -> terminal(player, state, current, MessageKey.RESOURCEPACK_KICK_FAILED, "paket indirilemedi");
            case INVALID_URL -> terminal(player, state, current, MessageKey.RESOURCEPACK_KICK_INVALID_URL, "adres gecersiz");
            case FAILED_RELOAD -> terminal(player, state, current, MessageKey.RESOURCEPACK_KICK_FAILED, "paket yeniden yuklenemedi");
            // Also arrives after a successful load, on pack removal and on
            // disconnect. The compare-and-set below is what stops that from
            // kicking a player who is already APPLIED.
            case DISCARDED -> terminal(player, state, current, MessageKey.RESOURCEPACK_KICK_DISCARDED, "paket birakildi");
        }
    }

    /**
     * A terminal failure. When the pack is not {@code zorunlu}, the player keeps
     * their seat and is told once, in chat, that their screen will look plainer
     * - the pack is cosmetic, and a soft launch has to be possible.
     */
    private void terminal(Player player, ResourcePackState state, ResourcePackSettings current,
                          MessageKey reason, String consoleReason) {
        if (!state.resolve()) {
            return;
        }
        if (!current.required()) {
            logger.info("Kaynak paketi uygulanmadi - " + player.getName() + ": " + consoleReason
                    + " (zorunlu degil, oyuncu iceride kaldi).");
            player.getScheduler().run(plugin,
                    task -> messages.send(player, MessageKey.RESOURCEPACK_OPTIONAL_WARNING), null);
            return;
        }
        fail(player, reason, current, consoleReason);
    }

    /**
     * Kicks with a two-part message: the specific reason, then the standing
     * instruction. Rendered rather than sent, since a kick screen is not chat
     * and must not carry the chat prefix.
     */
    private void fail(Player player, MessageKey reason, ResourcePackSettings current, String consoleReason) {
        Component message = messages.render(reason, Map.of("adres", current.url()))
                .append(Component.newline())
                .append(Component.newline())
                .append(messages.render(MessageKey.RESOURCEPACK_HINT));

        logger.info("Kaynak paketi basarisiz - " + player.getName() + ": " + consoleReason + ".");
        player.getScheduler().run(plugin, task -> player.kick(message), null);
    }

    @Override
    public void forget(UUID playerId) {
        ResourcePackState state = states.remove(playerId);
        if (state != null) {
            state.resolve();
        }
    }

    @Override
    public Optional<ResourcePackSnapshot> snapshot(UUID playerId) {
        return Optional.ofNullable(states.get(playerId)).map(ResourcePackState::toSnapshot);
    }
}
