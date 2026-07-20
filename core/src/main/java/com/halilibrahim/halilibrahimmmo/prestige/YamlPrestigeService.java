package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** YAML-backed prestige service that preserves all non-global profile progress. */
public final class YamlPrestigeService implements PrestigeService {
    private static final String FILE_NAME = "prestige.yml";
    private final JavaPlugin plugin; private final ProfileService profiles; private final ProgressionService progression;
    private final AtomicReference<PrestigeSettings> settings = new AtomicReference<>();
    public YamlPrestigeService(JavaPlugin plugin, ProfileService profiles, ProgressionService progression) { this.plugin = plugin; this.profiles = profiles; this.progression = progression; }
    @Override public void enable() { reload(); }
    @Override public void disable() { }
    @Override public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME); if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        try { int maximum = YamlConfiguration.loadConfiguration(file).getInt("maksimum-prestij", 0); if (maximum < 1) throw new IllegalArgumentException("Maximum prestige must be positive."); settings.set(new PrestigeSettings(maximum)); }
        catch (RuntimeException exception) { throw new LifecycleException("prestige.yml could not be loaded.", exception); }
    }
    @Override public PrestigeSettings settings() { PrestigeSettings value = settings.get(); if (value == null) throw new IllegalStateException("PrestigeService.enable() has not run yet."); return value; }
    @Override public CompletableFuture<PrestigeResult> prestige(UUID playerId, String playerName) {
        return profiles.load(playerId, playerName).thenCompose(profile -> {
            PrestigeResult.Status status = PrestigeRules.eligibility(profile, progression.settings().curve().maximumLevel(), settings().maximumPrestige());
            if (status != PrestigeResult.Status.PRESTIGED) return CompletableFuture.completedFuture(new PrestigeResult(status, profile.prestige()));
            return profiles.update(playerId, playerName, PrestigeRules::apply).thenApply(updated -> new PrestigeResult(PrestigeResult.Status.PRESTIGED, updated.prestige()));
        });
    }
}
