package com.halilibrahim.halilibrahimmmo.server;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/** YAML-backed {@link ServerInfoService}, following the ship-and-copy rule every other config service uses. */
public final class YamlServerInfoService implements ServerInfoService {

    private static final String FILE_NAME = "sunucu.yml";
    private static final long TICKS_PER_SECOND = 20L;

    private final JavaPlugin plugin;
    private final AtomicReference<ServerSettings> settings = new AtomicReference<>();

    public YamlServerInfoService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
    }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            long refreshSeconds = yaml.getLong("tab-listesi.yenileme-saniye", 2);
            if (refreshSeconds < 1) {
                throw new IllegalArgumentException("tab-listesi.yenileme-saniye must be at least one second.");
            }
            settings.set(new ServerSettings(
                    yaml.getBoolean("uyku.tek-kisi-yeter", true),
                    yaml.getBoolean("uyku.hava-durumunu-temizle", true),
                    yaml.getBoolean("uyku.duyuru-yapilsin", true),
                    yaml.getBoolean("tab-listesi.etkin", true),
                    refreshSeconds * TICKS_PER_SECOND
            ));
        } catch (RuntimeException exception) {
            throw new LifecycleException("sunucu.yml could not be loaded.", exception);
        }
    }

    @Override
    public ServerSettings settings() {
        ServerSettings current = settings.get();
        if (current == null) {
            throw new IllegalStateException("ServerInfoService.enable() has not run yet.");
        }
        return current;
    }
}
