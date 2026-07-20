package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.api.progression.ProgressionCause;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Global capped XP progression and its YAML-authored reward values. */
public final class YamlProgressionService implements ProgressionService {

    private static final String FILE_NAME = "progression.yml";

    private final JavaPlugin plugin;
    private final ProfileService profiles;
    private final AtomicReference<ProgressionSettings> settings = new AtomicReference<>();

    public YamlProgressionService(JavaPlugin plugin, ProfileService profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    @Override
    public void enable() { reload(); }

    @Override
    public void disable() { }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int maximumLevel = yaml.getInt("maksimum-seviye", 100);
        ConfigurationSection levels = yaml.getConfigurationSection("seviyeler");
        if (levels == null) throw new IllegalArgumentException("progression.yml must contain seviyeler.");
        Map<Integer, Long> requirements = new LinkedHashMap<>();
        for (String key : levels.getKeys(false)) requirements.put(Integer.parseInt(key), levels.getLong(key));
        Map<ProgressionCause, Long> rewards = new EnumMap<>(ProgressionCause.class);
        for (ProgressionCause cause : ProgressionCause.values()) {
            rewards.put(cause, Math.max(0L, yaml.getLong("oduller." + cause.name(), 0L)));
        }
        settings.set(new ProgressionSettings(new ExperienceCurve(maximumLevel, requirements), Map.copyOf(rewards)));
    }

    @Override
    public ProgressionSettings settings() { return settings.get(); }

    @Override
    public CompletableFuture<ProgressionAwardResult> award(UUID playerId, String playerName, long amount, ProgressionCause cause) {
        if (amount <= 0) {
            return profiles.load(playerId, playerName).thenApply(profile -> new ProgressionAwardResult(profile.snapshot(), false));
        }
        ExperienceCurve curve = settings().curve();
        AtomicInteger previousLevel = new AtomicInteger();
        return profiles.update(playerId, playerName, profile -> {
            previousLevel.set(profile.globalLevel());
            return curve.award(profile, amount);
        }).thenApply(updated -> new ProgressionAwardResult(updated.snapshot(), updated.globalLevel() != previousLevel.get()));
    }

    @Override
    public CompletableFuture<ProgressionAwardResult> awardConfigured(UUID playerId, String playerName, ProgressionCause cause) {
        return award(playerId, playerName, settings().rewardFor(cause), cause);
    }
}
