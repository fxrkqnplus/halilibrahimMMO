package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import com.halilibrahim.halilibrahimmmo.reputation.ReputationService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * YAML-backed prestige service. On success, resets only the player's
 * currently-selected class's class-level and all 4 skill levels back to
 * 1/0, banks the player's pre-reset Güç so it never drops, and grants +25%
 * XP per prestige rank plus a per-class permanent boost (see
 * {@code PROJECT_MEMORY.md} Phases 11-12).
 */
public final class YamlPrestigeService implements PrestigeService {
    private static final String FILE_NAME = "prestige.yml";
    private final JavaPlugin plugin;
    private final ProfileService profiles;
    private final ProgressionService progression;
    private final SkillService skills;
    private final ClassService classes;
    private final BankedPowerService bankedPower;
    private final ReputationService reputation;
    private final AtomicReference<PrestigeSettings> settings = new AtomicReference<>();

    public YamlPrestigeService(
            JavaPlugin plugin, ProfileService profiles, ProgressionService progression,
            SkillService skills, ClassService classes, BankedPowerService bankedPower, ReputationService reputation
    ) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.progression = progression;
        this.skills = skills;
        this.classes = classes;
        this.bankedPower = bankedPower;
        this.reputation = reputation;
    }

    @Override public void enable() { reload(); }
    @Override public void disable() { }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int maximum = yaml.getInt("maksimum-prestij", 0);
            int minimumSkillLevel = yaml.getInt("gerekli-beceri-seviyesi", 0);
            long reputationReward = yaml.getLong("prestij-sayginlik-odulu", 0);
            if (maximum < 1 || minimumSkillLevel < 1) throw new IllegalArgumentException("Maximum prestige and required skill level must be positive.");
            if (reputationReward < 0) throw new IllegalArgumentException("Prestige reputation reward cannot be negative.");
            settings.set(new PrestigeSettings(maximum, minimumSkillLevel, reputationReward, parseRewardValues(yaml)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("prestige.yml could not be loaded.", exception);
        }
    }

    /**
     * Every {@link PrestigeRewardType} must carry a positive value. A reward
     * worth nothing is a button that lies about what it does, and the screen
     * offers all four unconditionally - so a missing entry fails the load
     * rather than shipping a dead choice.
     */
    private Map<PrestigeRewardType, Double> parseRewardValues(YamlConfiguration yaml) {
        Map<PrestigeRewardType, Double> values = new EnumMap<>(PrestigeRewardType.class);
        for (PrestigeRewardType type : PrestigeRewardType.values()) {
            double value = yaml.getDouble("kalici-oduller." + type.name(), -1.0D);
            if (value <= 0) {
                throw new IllegalArgumentException("prestige.yml must declare a positive kalici-oduller." + type.name());
            }
            values.put(type, value);
        }
        return Map.copyOf(values);
    }

    @Override
    public PrestigeSettings settings() {
        PrestigeSettings value = settings.get();
        if (value == null) throw new IllegalStateException("PrestigeService.enable() has not run yet.");
        return value;
    }

    @Override
    public CompletableFuture<PrestigeResult> prestige(UUID playerId, String playerName) {
        return profiles.load(playerId, playerName).thenCompose(profile -> {
            PrestigeEligibility eligibility = PrestigeRules.eligibility(buildInput(playerId, profile));
            if (eligibility.status() != PrestigeResult.Status.PRESTIGED) {
                return CompletableFuture.completedFuture(new PrestigeResult(eligibility.status(), profile.prestige(), Optional.of(eligibility.report())));
            }
            String selectedClass = profile.selectedClass();
            int preResetGuc = progression.cachedGlobalLevel(playerId);
            return bankedPower.set(playerId, preResetGuc)
                    .thenCompose(ignored -> profiles.update(playerId, playerName, PrestigeRules::apply))
                    .thenCompose(updated -> skills.resetAll(playerId)
                            .thenCompose(ignoredReset -> resetSelectedClassLevel(playerId, playerName, selectedClass))
                            // Reputation is added, never reset: the score records what
                            // this player has taken out of the world, and a
                            // prestige does not undo any of it.
                            .thenCompose(ignoredReset -> reputation.award(playerId, settings().reputationReward()))
                            .thenApply(ignoredAward -> new PrestigeResult(PrestigeResult.Status.PRESTIGED, updated.prestige(), Optional.empty())));
        });
    }

    @Override
    public CompletableFuture<PrestigeEligibilityReport> eligibility(UUID playerId, String playerName) {
        return profiles.load(playerId, playerName)
                .thenApply(profile -> PrestigeRules.eligibility(buildInput(playerId, profile)).report());
    }

    private PrestigeEligibilityInput buildInput(UUID playerId, PlayerProfile profile) {
        Map<String, Integer> skillLevels = new LinkedHashMap<>();
        for (SkillDefinition definition : skills.definitions()) {
            skillLevels.put(definition.id(), skills.cached(playerId, definition.id()).map(progress -> progress.level()).orElse(1));
        }
        String classId = profile.selectedClass();
        boolean classSelected = !classId.isEmpty();
        int classLevel = 0;
        int synergySkillLevel = 0;
        int synergySkillCap = skills.settings().curve().maximumLevel();
        if (classSelected) {
            classLevel = classes.classProgress(playerId, classId).level();
            PlayerClassDefinition definition = classes.settings().definitions().get(classId);
            if (definition != null) {
                // A class may power several skills (Tabiat Vârisi powers four).
                // The weakest one is what the single "synergy skill" condition
                // reports, so the requirement keeps meaning *all* of them are
                // there - and the level asked for is the class's own
                // prestij-sinerji-seviyesi, not a single global cap.
                synergySkillLevel = definition.synergySkillIds().stream()
                        .mapToInt(skillId -> skills.cached(playerId, skillId).map(progress -> progress.level()).orElse(1))
                        .min().orElse(1);
                synergySkillCap = definition.prestigeSynergyLevel();
            }
        }
        return new PrestigeEligibilityInput(
                Map.copyOf(skillLevels), settings().minimumSkillLevel(),
                classSelected, classLevel, classes.settings().classLevelCurve().maximumLevel(),
                synergySkillLevel, synergySkillCap,
                profile.prestige(), settings().maximumPrestige()
        );
    }

    private CompletableFuture<Void> resetSelectedClassLevel(UUID playerId, String playerName, String selectedClass) {
        if (selectedClass.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return classes.resetClassLevel(playerId, playerName, selectedClass);
    }
}
