package com.halilibrahim.halilibrahimmmo.config;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads {@code config.yml} using Paper's built-in {@link YamlConfiguration}.
 *
 * <p>The bundled resource (with Turkish comments) is copied to the data folder
 * verbatim on first run and is never programmatically rewritten afterwards, so
 * admin edits and comments always survive {@code /haliladmin}'s reload. Every load produces
 * a brand new immutable {@link CoreConfig}, atomically published through an
 * {@link AtomicReference} so concurrent readers on other threads never observe a
 * half-parsed configuration.</p>
 */
public final class YamlConfigService implements ConfigService {

    private static final String FILE_NAME = "config.yml";

    private final JavaPlugin plugin;
    private final PluginLogger logger;
    private final AtomicReference<CoreConfig> current = new AtomicReference<>();

    public YamlConfigService(JavaPlugin plugin, PluginLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Override
    public void enable() {
        loadFromDisk();
    }

    @Override
    public void disable() {
        // Nothing to release - the parsed config is just an in-memory snapshot.
    }

    @Override
    public void reload() {
        loadFromDisk();
    }

    @Override
    public CoreConfig coreConfig() {
        CoreConfig config = current.get();
        if (config == null) {
            throw new IllegalStateException("ConfigService.enable() has not run yet.");
        }
        return config;
    }

    @Override
    public void setDebugModeOverride(boolean enabled) {
        current.updateAndGet(config -> config.withDebugMode(enabled));
    }

    private void loadFromDisk() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        current.set(parse(yaml));
    }

    private CoreConfig parse(YamlConfiguration yaml) {
        String locale = yaml.getString("genel.dil", "tr");
        boolean debugMode = yaml.getBoolean("genel.hata-ayiklama-modu", false);
        DatabaseSettings databaseSettings = parseDatabaseSettings(yaml);
        return new CoreConfig(locale, debugMode, databaseSettings);
    }

    private DatabaseSettings parseDatabaseSettings(YamlConfiguration yaml) {
        DatabaseType type = parseDatabaseType(yaml.getString("veritabani.tur", "SQLITE"));

        return new DatabaseSettings(
                type,
                yaml.getString("veritabani.sqlite.dosya-adi", "veritabani.db"),
                yaml.getString("veritabani.sunucu.host", "localhost"),
                yaml.getInt("veritabani.sunucu.port", 3306),
                yaml.getString("veritabani.sunucu.veritabani-adi", "halilibrahimmmo"),
                yaml.getString("veritabani.sunucu.kullanici-adi", "root"),
                yaml.getString("veritabani.sunucu.sifre", ""),
                yaml.getBoolean("veritabani.sunucu.ssl-kullan", false),
                yaml.getInt("veritabani.havuz.maksimum-baglanti", 10),
                yaml.getInt("veritabani.havuz.minimum-bosta-baglanti", 2),
                yaml.getLong("veritabani.havuz.baglanti-zaman-asimi-ms", 30_000L),
                yaml.getLong("veritabani.havuz.bosta-zaman-asimi-ms", 600_000L),
                yaml.getLong("veritabani.havuz.maksimum-yasam-suresi-ms", 1_800_000L)
        );
    }

    private DatabaseType parseDatabaseType(String raw) {
        try {
            return DatabaseType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            logger.warn("config.yml icindeki 'veritabani.tur' degeri gecersiz: '" + raw
                    + "'. SQLITE varsayilanina donuluyor.");
            return DatabaseType.SQLITE;
        }
    }
}
