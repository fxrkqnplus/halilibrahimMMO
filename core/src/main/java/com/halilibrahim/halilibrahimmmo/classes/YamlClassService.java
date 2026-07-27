package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.api.classes.PlayerClassSnapshot;
import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** YAML-backed class catalogue plus asynchronous persistent talent allocation. */
public final class YamlClassService implements ClassService {
    private static final String FILE_NAME = "classes.yml";
    private final JavaPlugin plugin;
    private final ProfileService profiles;
    private final ClassRepository repository;
    private final AbilityService abilities;
    private final SkillService skills;
    private final ProgressionService progression;
    private final AtomicReference<ClassSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, PlayerClassProgress> progressCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> selectedClasses = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateChains = new ConcurrentHashMap<>();
    private final NamespacedKey healthModifierKey;

    public YamlClassService(
            JavaPlugin plugin, ProfileService profiles, ClassRepository repository, AbilityService abilities,
            SkillService skills, ProgressionService progression
    ) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.repository = repository;
        this.abilities = abilities;
        this.skills = skills;
        this.progression = progression;
        this.healthModifierKey = new NamespacedKey(plugin, "class-health");
    }

    @Override public void enable() { reload(); }
    @Override public void disable() { progressCache.clear(); selectedClasses.clear(); updateChains.clear(); }

    @Override
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) plugin.saveResource(FILE_NAME, false);
        try {
            settings.set(parse(YamlConfiguration.loadConfiguration(file)));
        } catch (RuntimeException exception) {
            throw new LifecycleException("classes.yml could not be loaded.", exception);
        }
    }

    @Override public ClassSettings settings() {
        ClassSettings value = settings.get();
        if (value == null) throw new IllegalStateException("ClassService.enable() has not run yet.");
        return value;
    }
    @Override public Collection<PlayerClassDefinition> definitions() { return settings().definitions().values(); }
    @Override public String displayName(String classId) {
        PlayerClassDefinition definition = settings().definitions().get(classId);
        return definition == null ? classId : definition.displayName();
    }

    @Override
    public CompletableFuture<PlayerClassProgress> load(UUID playerId, String playerName) {
        PlayerClassProgress cached = progressCache.get(playerId);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return profiles.load(playerId, playerName).thenCompose(profile -> repository.load(playerId).thenApply(progress -> {
            selectedClasses.put(playerId, profile.selectedClass());
            progressCache.put(playerId, progress);
            return progress;
        }));
    }

    @Override
    public CompletableFuture<ClassSelectionResult> select(UUID playerId, String playerName, String rawClassId) {
        String classId = rawClassId.toLowerCase(Locale.ROOT);
        if (!settings().definitions().containsKey(classId)) {
            return CompletableFuture.completedFuture(new ClassSelectionResult(ClassSelectionResult.Status.UNKNOWN_CLASS, classId, 0));
        }
        return serial(playerId, () -> selectNow(playerId, playerName, classId));
    }

    private CompletableFuture<ClassSelectionResult> selectNow(UUID playerId, String playerName, String classId) {
        return profiles.load(playerId, playerName).thenCompose(profile -> load(playerId, playerName).thenCompose(progress -> {
            if (profile.selectedClass().equals(classId)) {
                return CompletableFuture.completedFuture(new ClassSelectionResult(ClassSelectionResult.Status.ALREADY_SELECTED, classId, 0));
            }
            long now = System.currentTimeMillis();
            if (!profile.selectedClass().isEmpty() && progress.classChangeReadyAtMillis() > now) {
                return CompletableFuture.completedFuture(new ClassSelectionResult(ClassSelectionResult.Status.COOLDOWN, classId, progress.classChangeReadyAtMillis() - now));
            }
            long readyAt = profile.selectedClass().isEmpty() ? 0L : Math.addExact(now, settings().changeCooldownMillis());
            return profiles.update(playerId, playerName, old -> withClass(old, classId))
                    .thenCompose(ignored -> repository.saveCooldown(playerId, readyAt))
                    .thenApply(ignored -> {
                        selectedClasses.put(playerId, classId);
                        progressCache.put(playerId, new PlayerClassProgress(playerId, progress.talentRanks(), readyAt, progress.classLevels()));
                        return new ClassSelectionResult(ClassSelectionResult.Status.SELECTED, classId, 0);
                    });
        }));
    }

    @Override
    public CompletableFuture<TalentSpendResult> spend(UUID playerId, String playerName, String rawTalentId) {
        String talentId = rawTalentId.toLowerCase(Locale.ROOT);
        return serial(playerId, () -> spendNow(playerId, playerName, talentId));
    }

    private CompletableFuture<TalentSpendResult> spendNow(UUID playerId, String playerName, String talentId) {
        return profiles.load(playerId, playerName).thenCompose(profile -> load(playerId, playerName).thenCompose(progress -> {
            String classId = profile.selectedClass();
            if (classId.isEmpty()) return CompletableFuture.completedFuture(new TalentSpendResult(TalentSpendResult.Status.NO_CLASS, talentId, 0, 0));
            PlayerClassDefinition definition = settings().definitions().get(classId);
            TalentNodeDefinition talent = definition == null ? null : definition.talents().get(talentId);
            if (talent == null) return CompletableFuture.completedFuture(new TalentSpendResult(TalentSpendResult.Status.UNKNOWN_TALENT, talentId, 0, 0));
            int currentRank = progress.talentRanks().getOrDefault(classId, Map.of()).getOrDefault(talentId, 0);
            int available = availablePoints(profile, progress, talent.effectType().category());
            if (currentRank >= talent.maximumRank()) return CompletableFuture.completedFuture(new TalentSpendResult(TalentSpendResult.Status.MAXIMUM_RANK, talentId, currentRank, available));
            if (available <= 0) return CompletableFuture.completedFuture(new TalentSpendResult(TalentSpendResult.Status.NO_POINTS, talentId, currentRank, available));
            int nextRank = currentRank + 1;
            return repository.saveRank(playerId, classId, talentId, nextRank).thenApply(ignored -> {
                Map<String, Map<String, Integer>> ranks = mutableCopy(progress.talentRanks());
                ranks.computeIfAbsent(classId, ignoredId -> new LinkedHashMap<>()).put(talentId, nextRank);
                PlayerClassProgress next = new PlayerClassProgress(playerId, freeze(ranks), progress.classChangeReadyAtMillis(), progress.classLevels());
                progressCache.put(playerId, next);
                return new TalentSpendResult(TalentSpendResult.Status.SPENT, talentId, nextRank, available - 1);
            });
        }));
    }

    @Override
    public CompletableFuture<Optional<PlayerClassSnapshot>> findSnapshot(UUID playerId) {
        return profiles.find(playerId).thenCompose(found -> found.map(profile -> load(playerId, profile.playerName()).thenApply(progress -> Optional.of(snapshot(profile, progress))))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())));
    }

    @Override
    public ClassModifiers modifiers(UUID playerId) {
        String classId = selectedClasses.get(playerId);
        PlayerClassProgress progress = progressCache.get(playerId);
        PlayerClassDefinition definition = classId == null || progress == null ? null : settings().definitions().get(classId);
        if (definition == null) return ClassModifiers.none();
        double health = 0, damage = 0;
        int energy = 0;
        Map<String, Double> costs = new LinkedHashMap<>(), cooldowns = new LinkedHashMap<>();
        Map<String, Integer> limits = new LinkedHashMap<>();
        Map<String, Double> durations = new LinkedHashMap<>(), effects = new LinkedHashMap<>();
        Map<String, Integer> ranks = progress.talentRanks().getOrDefault(classId, Map.of());
        for (TalentNodeDefinition talent : definition.talents().values()) {
            int rank = ranks.getOrDefault(talent.id(), 0);
            double total = rank * talent.valuePerRank();
            switch (talent.effectType()) {
                case MAX_HEALTH -> health += total;
                case PVE_DAMAGE_MULTIPLIER -> damage += total;
                case MAX_ENERGY -> energy += (int) total;
                case ABILITY_ENERGY_COST_REDUCTION -> costs.merge(talent.abilityId(), total, Double::sum);
                case ABILITY_COOLDOWN_REDUCTION -> cooldowns.merge(talent.abilityId(), total, Double::sum);
                case ABILITY_LIMIT_BONUS -> limits.merge(talent.abilityId(), (int) total, Integer::sum);
                case ABILITY_DURATION_BONUS -> durations.merge(talent.abilityId(), total, Double::sum);
                case ABILITY_EFFECT_BONUS -> effects.merge(talent.abilityId(), total, Double::sum);
            }
        }
        int prestigeCount = profiles.cached(playerId).map(PlayerProfile::prestige).orElse(0);
        double prestigeTotal = prestigeCount * definition.prestigeEffectValuePerRank();
        switch (definition.prestigeEffectType()) {
            case MAX_HEALTH -> health += prestigeTotal;
            case PVE_DAMAGE_MULTIPLIER -> damage += prestigeTotal;
            case MAX_ENERGY -> energy += (int) prestigeTotal;
            case ABILITY_ENERGY_COST_REDUCTION, ABILITY_COOLDOWN_REDUCTION, ABILITY_LIMIT_BONUS,
                 ABILITY_DURATION_BONUS, ABILITY_EFFECT_BONUS -> {
                // Prestige boosts are deliberately restricted to MAX_HEALTH/PVE_DAMAGE_MULTIPLIER/MAX_ENERGY
                // at parse time (see parse(...)) - per-ability modifiers need an ability id this
                // class-wide bonus does not have.
            }
        }
        return new ClassModifiers(health, damage, energy, Map.copyOf(costs), Map.copyOf(cooldowns), Map.copyOf(limits),
                Map.copyOf(durations), Map.copyOf(effects));
    }

    @Override
    public void applyPlayerEffects(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) return;
        attribute.getModifiers().stream().filter(modifier -> modifier.getKey().equals(healthModifierKey)).toList().forEach(attribute::removeModifier);
        double bonus = modifiers(player.getUniqueId()).maximumHealthBonus();
        if (bonus != 0) attribute.addModifier(new AttributeModifier(healthModifierKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
        player.setHealth(Math.min(player.getHealth(), attribute.getValue()));
    }

    @Override public void evict(UUID playerId) { progressCache.remove(playerId); selectedClasses.remove(playerId); }

    @Override
    public CompletableFuture<ClassExperienceAwardResult> awardClassExperience(UUID playerId, String playerName, String classId, long amount) {
        return serial(playerId, () -> profiles.load(playerId, playerName).thenCompose(profile -> {
            double prestigeMultiplier = 1.0 + 0.25 * profile.prestige();
            long adjustedAmount = Math.round(amount * prestigeMultiplier);
            return applyClassExperience(playerId, playerName, classId, adjustedAmount);
        }));
    }

    /**
     * Awards class XP without applying the prestige multiplier - the one
     * caller of this method, {@code ClassSynergyBridge}, mirrors an amount
     * that already went through the skill-side prestige bonus
     * ({@code classSynergyMultiplier} in the plugin bootstrap); reapplying it
     * here would compound prestige quadratically for synergy-skill class XP
     * instead of the single +25%-per-rank bonus every other XP source gets.
     */
    @Override
    public CompletableFuture<ClassExperienceAwardResult> awardClassExperienceExact(UUID playerId, String playerName, String classId, long amount) {
        return serial(playerId, () -> applyClassExperience(playerId, playerName, classId, amount));
    }

    private CompletableFuture<ClassExperienceAwardResult> applyClassExperience(UUID playerId, String playerName, String classId, long adjustedAmount) {
        return load(playerId, playerName).thenCompose(progress -> {
            ClassLevelProgress before = progress.levelProgress(classId);
            ClassLevelProgress after = settings().classLevelCurve().award(before, adjustedAmount);
            Map<String, ClassLevelProgress> updatedLevels = new LinkedHashMap<>(progress.classLevels());
            updatedLevels.put(classId, after);
            PlayerClassProgress next = new PlayerClassProgress(playerId, progress.talentRanks(), progress.classChangeReadyAtMillis(), Map.copyOf(updatedLevels));
            return repository.saveClassLevel(playerId, classId, after.level(), after.experience()).thenApply(ignored -> {
                progressCache.put(playerId, next);
                return new ClassExperienceAwardResult(before.level(), after.level(), after.level() != before.level());
            });
        });
    }

    @Override
    public ClassLevelProgress classProgress(UUID playerId, String classId) {
        PlayerClassProgress progress = progressCache.get(playerId);
        return progress == null ? new ClassLevelProgress(classId, 1, 0L) : progress.levelProgress(classId);
    }

    @Override
    public CompletableFuture<Void> resetClassLevel(UUID playerId, String playerName, String classId) {
        return serial(playerId, () -> load(playerId, playerName).thenCompose(progress -> {
            ClassLevelProgress reset = new ClassLevelProgress(classId, 1, 0L);
            Map<String, ClassLevelProgress> updatedLevels = new LinkedHashMap<>(progress.classLevels());
            updatedLevels.put(classId, reset);
            PlayerClassProgress next = new PlayerClassProgress(playerId, progress.talentRanks(), progress.classChangeReadyAtMillis(), Map.copyOf(updatedLevels));
            return repository.saveClassLevel(playerId, classId, reset.level(), reset.experience()).thenAccept(ignored -> progressCache.put(playerId, next));
        }));
    }

    @Override
    public CompletableFuture<ClassExperienceAwardResult> adminSetClassLevel(UUID playerId, String playerName, String classId, int level) {
        return serial(playerId, () -> load(playerId, playerName).thenCompose(progress -> {
            int clamped = Math.max(1, Math.min(level, settings().classLevelCurve().maximumLevel()));
            int previousLevel = progress.levelProgress(classId).level();
            ClassLevelProgress target = new ClassLevelProgress(classId, clamped, 0L);
            Map<String, ClassLevelProgress> updatedLevels = new LinkedHashMap<>(progress.classLevels());
            updatedLevels.put(classId, target);
            PlayerClassProgress next = new PlayerClassProgress(playerId, progress.talentRanks(), progress.classChangeReadyAtMillis(), Map.copyOf(updatedLevels));
            return repository.saveClassLevel(playerId, classId, target.level(), target.experience()).thenApply(ignored -> {
                progressCache.put(playerId, next);
                return new ClassExperienceAwardResult(previousLevel, clamped, clamped != previousLevel);
            });
        }));
    }

    @Override
    public CompletableFuture<ClassExperienceAwardResult> adminAddClassExperience(UUID playerId, String playerName, String classId, long amount) {
        return serial(playerId, () -> load(playerId, playerName).thenCompose(progress -> {
            ClassLevelProgress before = progress.levelProgress(classId);
            ClassLevelProgress after = settings().classLevelCurve().award(before, amount);
            Map<String, ClassLevelProgress> updatedLevels = new LinkedHashMap<>(progress.classLevels());
            updatedLevels.put(classId, after);
            PlayerClassProgress next = new PlayerClassProgress(playerId, progress.talentRanks(), progress.classChangeReadyAtMillis(), Map.copyOf(updatedLevels));
            return repository.saveClassLevel(playerId, classId, after.level(), after.experience()).thenApply(ignored -> {
                progressCache.put(playerId, next);
                return new ClassExperienceAwardResult(before.level(), after.level(), after.level() != before.level());
            });
        }));
    }

    @Override
    public CompletableFuture<Void> adminForceSetClass(UUID playerId, String playerName, String classId) {
        return serial(playerId, () -> load(playerId, playerName).thenCompose(progress ->
                profiles.update(playerId, playerName, old -> withClass(old, classId)).thenAccept(ignored -> {
                    selectedClasses.put(playerId, classId);
                    progressCache.put(playerId, new PlayerClassProgress(playerId, progress.talentRanks(), progress.classChangeReadyAtMillis(), progress.classLevels()));
                })));
    }

    @Override
    public int pointsFor(PlayerProfile profile, TalentPointCategory category) {
        return TalentPointBudget.pointsFor(
                progression.cachedGlobalLevel(profile.playerId()), profile.prestige(), settings().talentPoints(), category
        );
    }

    @Override
    public int availablePoints(PlayerProfile profile, PlayerClassProgress progress, TalentPointCategory category) {
        return pointsFor(profile, category) - progress.spentPoints(settings().definitions(), category);
    }

    private ClassSettings parse(YamlConfiguration yaml) {
        long cooldownDays = yaml.getLong("sinif-degistirme-bekleme-gun", -1);
        if (cooldownDays < 0) throw new IllegalArgumentException("Invalid class change cooldown setting.");
        ConfigurationSection points = requiredSection(yaml, "yetenek-puani");
        TalentPointSettings talentPoints = new TalentPointSettings(
                points.getInt("ozellik-seviye-basina", 0), points.getInt("kabiliyet-seviye-basina", 0),
                points.getInt("prestij-basina-ozellik", -1), points.getInt("prestij-basina-kabiliyet", -1)
        );
        if (talentPoints.levelsPerFeaturePoint() < 1 || talentPoints.levelsPerAbilityPoint() < 1
                || talentPoints.featurePointsPerPrestige() < 0 || talentPoints.abilityPointsPerPrestige() < 0) {
            throw new IllegalArgumentException("Invalid talent point settings.");
        }
        ConfigurationSection progressionSection = requiredSection(yaml, "sinif-ilerleme");
        int classMaximumLevel = progressionSection.getInt("maksimum-seviye", 0);
        double synergyBonus = progressionSection.getDouble("sinerji-xp-bonusu", 0);
        if (synergyBonus < 0) throw new IllegalArgumentException("Class synergy XP bonus must not be negative.");
        ConfigurationSection classLevels = requiredSection(progressionSection, "seviyeler");
        Map<Integer, Long> classLevelRequirements = new LinkedHashMap<>();
        for (String key : classLevels.getKeys(false)) {
            classLevelRequirements.put(Integer.parseInt(key), classLevels.getLong(key));
        }
        ClassExperienceCurve classLevelCurve = new ClassExperienceCurve(classMaximumLevel, classLevelRequirements);
        ConfigurationSection classes = requiredSection(yaml, "siniflar");
        Map<String, PlayerClassDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : classes.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) throw new IllegalArgumentException("Invalid class id: " + rawId);
            ConfigurationSection section = requiredSection(classes, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Class display name required: " + id);
            Set<String> synergySkillIds = new LinkedHashSet<>();
            for (String rawSkillId : section.getStringList("sinerji-beceriler")) {
                String skillId = rawSkillId.toLowerCase(Locale.ROOT);
                if (!skills.settings().definitions().containsKey(skillId)) {
                    throw new IllegalArgumentException("Class synergy skill must reference a known skill id: " + id + "/" + rawSkillId);
                }
                synergySkillIds.add(skillId);
            }
            if (synergySkillIds.isEmpty()) {
                throw new IllegalArgumentException("Class must declare at least one sinerji-beceriler entry: " + id);
            }
            ClassBarColor barColor = ClassBarColor.valueOf(section.getString("bossbar-rengi", "").toUpperCase(Locale.ROOT));
            Material icon = Material.matchMaterial(section.getString("simge", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Class must declare a known simge material: " + id);
            }
            Material frame = Material.matchMaterial(section.getString("cerceve", ""));
            if (frame == null) {
                throw new IllegalArgumentException("Class must declare a known cerceve material: " + id);
            }
            ConfigurationSection talents = requiredSection(section, "yetenekler");
            Map<String, TalentNodeDefinition> nodes = new LinkedHashMap<>();
            for (String rawTalentId : talents.getKeys(false)) {
                String talentId = rawTalentId.toLowerCase(Locale.ROOT);
                ConfigurationSection node = requiredSection(talents, rawTalentId);
                ConfigurationSection effect = requiredSection(node, "etki");
                String nodeName = node.getString("gorunen-ad");
                int maximumRank = node.getInt("maksimum-rank", 0);
                ClassEffectType type = ClassEffectType.valueOf(effect.getString("tur", "").toUpperCase(Locale.ROOT));
                String abilityId = effect.getString("kabiliyet", "").toLowerCase(Locale.ROOT);
                double value = effect.getDouble("deger-rank-basina", 0);
                if (!talentId.matches("[a-z0-9_-]+") || nodes.containsKey(talentId) || nodeName == null || nodeName.isBlank() || maximumRank < 1 || maximumRank > 64 || value <= 0) throw new IllegalArgumentException("Invalid talent: " + rawTalentId);
                boolean needsAbility = type.requiresAbility();
                if (needsAbility != !abilityId.isBlank() || (needsAbility && !abilities.settings().definitions().containsKey(abilityId))) throw new IllegalArgumentException("Invalid talent ability: " + talentId);
                if ((type == ClassEffectType.ABILITY_ENERGY_COST_REDUCTION || type == ClassEffectType.ABILITY_COOLDOWN_REDUCTION) && value * maximumRank >= 1.0D) throw new IllegalArgumentException("Reduction talent cannot reach 100%: " + talentId);
                if ((type == ClassEffectType.MAX_ENERGY || type == ClassEffectType.ABILITY_LIMIT_BONUS) && value != Math.rint(value)) throw new IllegalArgumentException("Integer talent requires an integer value: " + talentId);
                nodes.put(talentId, new TalentNodeDefinition(talentId, nodeName, maximumRank, type, abilityId, value));
            }
            if (nodes.isEmpty()) throw new IllegalArgumentException("Class must define talents: " + id);
            ConfigurationSection prestigeEffect = requiredSection(section, "prestij-etkisi");
            ClassEffectType prestigeEffectType = ClassEffectType.valueOf(prestigeEffect.getString("tur", "").toUpperCase(Locale.ROOT));
            double prestigeEffectValue = prestigeEffect.getDouble("deger-basina", 0);
            boolean prestigeTypeAllowed = prestigeEffectType == ClassEffectType.MAX_HEALTH
                    || prestigeEffectType == ClassEffectType.PVE_DAMAGE_MULTIPLIER
                    || prestigeEffectType == ClassEffectType.MAX_ENERGY;
            if (!prestigeTypeAllowed || prestigeEffectValue <= 0) {
                throw new IllegalArgumentException("Class prestige effect must be a positive MAX_HEALTH/PVE_DAMAGE_MULTIPLIER/MAX_ENERGY value: " + id);
            }
            definitions.put(id, new PlayerClassDefinition(id, displayName, Set.copyOf(synergySkillIds), barColor, icon, frame, Map.copyOf(nodes), prestigeEffectType, prestigeEffectValue));
        }
        if (definitions.isEmpty()) throw new IllegalArgumentException("classes.yml must define classes.");
        return new ClassSettings(Math.multiplyExact(cooldownDays, 86_400_000L), talentPoints, classLevelCurve, synergyBonus, Map.copyOf(definitions));
    }

    /**
     * The public API keeps reporting one combined budget rather than gaining
     * two more fields: an external consumer asking "how many points does this
     * player have" is asking about their total investment capacity, and the
     * per-pool split is an internal spending rule, not part of the contract.
     */
    private PlayerClassSnapshot snapshot(PlayerProfile profile, PlayerClassProgress progress) {
        int points = pointsFor(profile, TalentPointCategory.OZELLIK) + pointsFor(profile, TalentPointCategory.KABILIYET);
        return new PlayerClassSnapshot(profile.playerId(), profile.selectedClass(), points, progress.spentPoints(), progress.talentRanks());
    }
    private static PlayerProfile withClass(PlayerProfile old, String classId) { return new PlayerProfile(old.playerId(), old.playerName(), old.globalLevel(), old.globalExperience(), old.prestige(), classId); }
    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) { ConfigurationSection section = parent.getConfigurationSection(path); if (section == null) throw new IllegalArgumentException("Missing YAML section: " + path); return section; }
    private static Map<String, Map<String, Integer>> mutableCopy(Map<String, Map<String, Integer>> source) { Map<String, Map<String, Integer>> copy = new LinkedHashMap<>(); source.forEach((key, value) -> copy.put(key, new LinkedHashMap<>(value))); return copy; }
    private static Map<String, Map<String, Integer>> freeze(Map<String, Map<String, Integer>> source) { Map<String, Map<String, Integer>> frozen = new LinkedHashMap<>(); source.forEach((key, value) -> frozen.put(key, Map.copyOf(value))); return Map.copyOf(frozen); }
    private <T> CompletableFuture<T> serial(UUID playerId, Supplier<CompletableFuture<T>> action) {
        synchronized (updateChains) {
            CompletableFuture<Void> previous = updateChains.getOrDefault(playerId, CompletableFuture.completedFuture(null));
            CompletableFuture<T> result = previous.handle((ignored, throwable) -> null).thenCompose(ignored -> action.get());
            CompletableFuture<Void> tail = result.handle((ignored, throwable) -> null);
            updateChains.put(playerId, tail);
            tail.whenComplete((ignored, throwable) -> updateChains.remove(playerId, tail));
            return result;
        }
    }
}
