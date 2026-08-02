package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.resourcepack.ItemModels;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
import java.util.function.BiFunction;

/** YAML-backed, write-through cache for all configured player skills. */
public final class CachedSkillService implements SkillService {

    private static final String FILE_NAME = "skills.yml";

    private final JavaPlugin plugin;
    private final SkillRepository repository;
    private final BiFunction<UUID, String, Double> classSynergyMultiplier;
    private final SkillExperienceListener experienceListener;
    private final AtomicReference<SkillSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<SkillKey, SkillProgress> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SkillKey, CompletableFuture<SkillProgress>> updateChains = new ConcurrentHashMap<>();
    private final NamespacedKey milestoneHealthKey;

    public CachedSkillService(
            JavaPlugin plugin, SkillRepository repository,
            BiFunction<UUID, String, Double> classSynergyMultiplier, SkillExperienceListener experienceListener
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.classSynergyMultiplier = classSynergyMultiplier;
        this.experienceListener = experienceListener;
        this.milestoneHealthKey = new NamespacedKey(plugin, "skill-milestone-health");
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        cache.values().forEach(repository::save);
        cache.clear();
        updateChains.clear();
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
            throw new LifecycleException("skills.yml could not be loaded.", exception);
        }
    }

    @Override
    public SkillSettings settings() {
        SkillSettings current = settings.get();
        if (current == null) {
            throw new IllegalStateException("SkillService.enable() has not run yet.");
        }
        return current;
    }

    @Override
    public Collection<SkillDefinition> definitions() {
        return settings().definitions().values();
    }

    @Override
    public CompletableFuture<Optional<SkillProgress>> find(UUID playerId, String skillId) {
        if (!settings().definitions().containsKey(skillId)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return load(playerId, skillId).thenApply(Optional::of);
    }

    @Override
    public CompletableFuture<SkillProgress> load(UUID playerId, String skillId) {
        SkillKey key = new SkillKey(playerId, skillId);
        SkillProgress cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findAll(playerId).thenApply(found -> {
            found.values().forEach(progress -> cache.put(new SkillKey(playerId, progress.skillId()), progress));
            return cache.computeIfAbsent(key, ignored -> new SkillProgress(playerId, skillId, 1, 0));
        });
    }

    @Override
    public CompletableFuture<Void> loadAll(UUID playerId) {
        return repository.findAll(playerId).thenAccept(found -> found.values()
                .forEach(progress -> cache.put(new SkillKey(playerId, progress.skillId()), progress)));
    }

    @Override
    public CompletableFuture<Optional<SkillAwardResult>> awardBlock(UUID playerId, String playerName, Material material) {
        return awardMatching(playerId, playerName, SkillSource.BLOCK, material, null);
    }

    @Override
    public CompletableFuture<Optional<SkillAwardResult>> awardMob(UUID playerId, String playerName, EntityType entityType) {
        return awardMatching(playerId, playerName, SkillSource.MOB, null, entityType);
    }

    @Override
    public CompletableFuture<Optional<SkillAwardResult>> awardAction(UUID playerId, String playerName, SkillAction action, Material context) {
        for (SkillDefinition skill : settings().definitions().values()) {
            SkillActionReward reward = skill.actionRewards().get(action);
            if (reward == null) {
                continue;
            }
            if (!reward.accepts(context)) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return award(playerId, playerName, skill, reward.xp(), reward.displayName());
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<SkillAwardResult>> awardExternal(
            UUID playerId, String playerName, String skillId, long amount, String sourceDisplayName
    ) {
        SkillDefinition skill = settings().definitions().get(skillId);
        if (skill == null || amount <= 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return award(playerId, playerName, skill, amount, sourceDisplayName);
    }

    @Override
    public Optional<SkillActionReward> actionReward(SkillAction action) {
        return settings().definitions().values().stream()
                .map(skill -> skill.actionRewards().get(action))
                .filter(reward -> reward != null)
                .findFirst();
    }

    @Override
    public Optional<SkillProgress> cached(UUID playerId, String skillId) {
        return Optional.ofNullable(cache.get(new SkillKey(playerId, skillId)));
    }

    @Override
    public CompletableFuture<Void> resetAll(UUID playerId) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (SkillDefinition definition : settings().definitions().values()) {
            SkillKey key = new SkillKey(playerId, definition.id());
            chain = chain.thenCompose(ignored -> setProgress(key, new SkillProgress(key.playerId(), key.skillId(), 1, 0)));
        }
        return chain;
    }

    @Override
    public CompletableFuture<SkillAdminAwardResult> adminSetLevel(UUID playerId, String skillId, int level) {
        int clamped = Math.max(1, Math.min(level, settings().curve().maximumLevel()));
        SkillKey key = new SkillKey(playerId, skillId);
        int previousLevel = cached(playerId, skillId).map(progress -> progress.level()).orElse(1);
        return setProgress(key, new SkillProgress(playerId, skillId, clamped, 0))
                .thenApply(ignored -> new SkillAdminAwardResult(previousLevel, clamped, clamped != previousLevel));
    }

    @Override
    public CompletableFuture<SkillAdminAwardResult> adminAddExperience(UUID playerId, String skillId, long amount) {
        SkillKey key = new SkillKey(playerId, skillId);
        synchronized (updateChains) {
            CompletableFuture<SkillProgress> previous = updateChains.getOrDefault(key, CompletableFuture.completedFuture(null));
            CompletableFuture<SkillProgress> base = previous.handle((ignored, throwable) -> null)
                    .thenCompose(ignored -> load(playerId, skillId));
            CompletableFuture<SkillAdminAwardResult> resultFuture = base.thenCompose(progress -> {
                int previousLevel = progress.level();
                SkillProgress awarded = settings().curve().award(progress, amount);
                return repository.save(awarded).thenApply(saved -> {
                    cache.put(key, awarded);
                    return new SkillAdminAwardResult(previousLevel, awarded.level(), awarded.level() != previousLevel);
                });
            });
            CompletableFuture<SkillProgress> tail = resultFuture.thenApply(ignored -> cache.get(key));
            updateChains.put(key, tail);
            tail.whenComplete((ignored, throwable) -> updateChains.remove(key, tail));
            return resultFuture;
        }
    }

    @Override
    public SkillMilestoneModifiers milestoneModifiers(UUID playerId) {
        double maxHealthBonus = 0;
        double pveDamageBonus = 0;
        int maxEnergyBonus = 0;
        int hasteAmplifier = -1;
        int speedAmplifier = -1;
        int treeFellingLimit = 0;
        double breedRefundChance = 0;
        double breedTwinChance = 0;
        for (SkillDefinition definition : settings().definitions().values()) {
            int level = cached(playerId, definition.id()).map(progress -> progress.level()).orElse(1);
            for (Map.Entry<Integer, SkillMilestoneEffect> entry : definition.milestones().entrySet()) {
                if (level < entry.getKey()) {
                    continue;
                }
                SkillMilestoneEffect effect = entry.getValue();
                switch (effect.type()) {
                    case MAX_HEALTH -> maxHealthBonus += effect.value();
                    case PVE_DAMAGE_MULTIPLIER -> pveDamageBonus += effect.value();
                    case MAX_ENERGY -> maxEnergyBonus += (int) effect.value();
                    case HASTE -> hasteAmplifier = Math.max(hasteAmplifier, (int) effect.value());
                    case SPEED -> speedAmplifier = Math.max(speedAmplifier, (int) effect.value());
                    case TREE_FELLING -> treeFellingLimit = Math.max(treeFellingLimit, (int) effect.value());
                    case BREED_REFUND -> breedRefundChance = Math.max(breedRefundChance, effect.value());
                    case BREED_TWIN -> breedTwinChance = Math.max(breedTwinChance, effect.value());
                    case DOUBLE_DROP, EXCAVATION, AUTO_REPLANT -> {
                        // Scoped to the granting skill's own blocks, so they are
                        // deliberately absent from this cross-skill aggregate -
                        // see milestoneValue(...).
                    }
                }
            }
        }
        return new SkillMilestoneModifiers(
                maxHealthBonus, pveDamageBonus, maxEnergyBonus, hasteAmplifier, speedAmplifier,
                treeFellingLimit, breedRefundChance, breedTwinChance
        );
    }

    /**
     * The highest reached tier's value for one specific skill and effect type,
     * or {@code 0} when nothing that skill grants matches. Highest-wins rather
     * than summed: two tiers of the same feature describe one capability at
     * two strengths, exactly as {@code TREE_FELLING} already behaves.
     */
    @Override
    public double milestoneValue(UUID playerId, String skillId, SkillMilestoneEffectType type) {
        SkillDefinition definition = settings().definitions().get(skillId);
        if (definition == null) {
            return 0;
        }
        int level = cached(playerId, skillId).map(progress -> progress.level()).orElse(1);
        double best = 0;
        for (Map.Entry<Integer, SkillMilestoneEffect> entry : definition.milestones().entrySet()) {
            if (level >= entry.getKey() && entry.getValue().type() == type) {
                best = Math.max(best, entry.getValue().value());
            }
        }
        return best;
    }

    /**
     * Which configured {@link SkillSource#BLOCK} skill claims {@code material},
     * if any. Milestone features that act on "this skill's own blocks" need to
     * answer that question from a bare {@code BlockBreakEvent}, and the reward
     * maps already are the single source of truth for it.
     */
    @Override
    public Optional<String> blockSkillId(Material material) {
        for (SkillDefinition definition : settings().definitions().values()) {
            if (definition.materialRewards().containsKey(material)) {
                return Optional.of(definition.id());
            }
        }
        return Optional.empty();
    }

    @Override
    public void applyMilestoneEffects(Player player) {
        SkillMilestoneModifiers modifiers = milestoneModifiers(player.getUniqueId());
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            attribute.getModifiers().stream().filter(modifier -> modifier.getKey().equals(milestoneHealthKey)).toList()
                    .forEach(attribute::removeModifier);
            if (modifiers.maxHealthBonus() != 0) {
                attribute.addModifier(new AttributeModifier(milestoneHealthKey, modifiers.maxHealthBonus(), AttributeModifier.Operation.ADD_NUMBER));
            }
            player.setHealth(Math.min(player.getHealth(), attribute.getValue()));
        }
        applyMilestonePotionEffect(player, PotionEffectType.HASTE, modifiers.hasteAmplifier());
        applyMilestonePotionEffect(player, PotionEffectType.SPEED, modifiers.speedAmplifier());
    }

    /**
     * Removes any previous milestone-driven potion effect of this type and
     * reapplies it at the currently-reached amplifier - idempotent, mirroring
     * the attribute-modifier remove-then-reapply trick above. Unlike
     * attribute modifiers, Bukkit potion effects carry no per-source key, so
     * this can also clear a genuinely player-drunk potion of the same type;
     * an accepted tradeoff since milestone recomputation only runs at
     * discrete moments (join/level-up/prestige), not continuously.
     */
    private void applyMilestonePotionEffect(Player player, PotionEffectType type, int amplifier) {
        player.removePotionEffect(type);
        if (amplifier >= 0) {
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifier, true, false, false));
        }
    }

    private CompletableFuture<Void> setProgress(SkillKey key, SkillProgress target) {
        synchronized (updateChains) {
            CompletableFuture<SkillProgress> previous = updateChains.getOrDefault(key, CompletableFuture.completedFuture(null));
            CompletableFuture<SkillProgress> tail = previous.handle((ignored, throwable) -> null)
                    .thenCompose(ignored -> repository.save(target).thenApply(saved -> target))
                    .thenApply(progress -> {
                        cache.put(key, progress);
                        return progress;
                    });
            updateChains.put(key, tail);
            tail.whenComplete((ignored, throwable) -> updateChains.remove(key, tail));
            return tail.thenAccept(ignored -> { });
        }
    }

    @Override
    public void evict(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private CompletableFuture<Optional<SkillAwardResult>> awardMatching(
            UUID playerId, String playerName, SkillSource source, Material material, EntityType entityType
    ) {
        Optional<SkillDefinition> definition = settings().definitions().values().stream()
                .filter(candidate -> candidate.source() == source)
                .filter(candidate -> source != SkillSource.BLOCK || candidate.materialRewards().containsKey(material))
                .findFirst();
        if (definition.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        SkillDefinition skill = definition.get();
        SkillRewardEntry rewardEntry = source == SkillSource.BLOCK
                ? skill.materialRewards().get(material)
                : skill.entityRewards().getOrDefault(entityType, skill.defaultEntityReward());
        return award(playerId, playerName, skill, rewardEntry.xp(), rewardEntry.displayName());
    }

    /**
     * The single write path every XP source funnels into: applies the caller's
     * class-synergy/prestige multiplier, chains the award behind this
     * (player, skill) pair's own serial update chain, persists it and notifies
     * the class-synergy bridge.
     */
    private CompletableFuture<Optional<SkillAwardResult>> award(
            UUID playerId, String playerName, SkillDefinition skill, long baseAmount, String sourceDisplayName
    ) {
        SkillKey key = new SkillKey(playerId, skill.id());
        double multiplier = classSynergyMultiplier.apply(playerId, skill.id());
        long grantedAmount = Math.round(baseAmount * multiplier);
        synchronized (updateChains) {
            CompletableFuture<SkillProgress> previous = updateChains.get(key);
            CompletableFuture<SkillProgress> base = previous == null ? load(playerId, skill.id()) : previous;
            CompletableFuture<SkillAwardResult> next = base.thenApply(progress -> {
                        int previousLevel = progress.level();
                        SkillProgress awarded = settings().curve().award(progress, grantedAmount);
                        return new SkillAwardResult(awarded, previousLevel, grantedAmount, awarded.level() != previousLevel, sourceDisplayName);
                    })
                    .thenCompose(result -> repository.save(result.progress()).thenApply(ignored -> result))
                    .thenApply(result -> {
                        cache.put(key, result.progress());
                        return result;
                    });
            CompletableFuture<SkillProgress> tail = next.thenApply(SkillAwardResult::progress);
            updateChains.put(key, tail);
            tail.whenComplete((ignored, throwable) -> updateChains.remove(key, tail));
            return next.thenApply(result -> {
                experienceListener.onSkillExperienceGained(playerId, playerName, skill.id(), grantedAmount);
                return Optional.of(result);
            });
        }
    }

    private SkillSettings parse(YamlConfiguration yaml) {
        int maximumLevel = yaml.getInt("maksimum-seviye", 100);
        ConfigurationSection levels = requireSection(yaml, "seviyeler");
        Map<Integer, Long> requirements = new LinkedHashMap<>();
        for (String key : levels.getKeys(false)) {
            requirements.put(Integer.parseInt(key), levels.getLong(key));
        }
        SkillExperienceCurve curve = new SkillExperienceCurve(maximumLevel, requirements);
        ConfigurationSection skills = requireSection(yaml, "beceriler");
        Map<String, SkillDefinition> definitions = new LinkedHashMap<>();
        Map<SkillAction, String> actionOwners = new LinkedHashMap<>();
        for (String rawId : skills.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate skill id: " + rawId);
            }
            ConfigurationSection section = requireSection(skills, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Skill display name is required: " + id);
            }
            Material icon = Material.matchMaterial(section.getString("simge", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Skill must declare a known simge material: " + id);
            }
            // Optional: names a texture the server resource pack ships.
            // Absent leaves the icon exactly as it was, which is what lets a
            // server keep its unregenerated YAML and still run this version.
            NamespacedKey model = ItemModels.parse(section);
            Map<Integer, SkillMilestoneEffect> milestones = parseMilestones(section, id);
            Map<SkillAction, SkillActionReward> actionRewards = parseActions(section, id, actionOwners);
            SkillSource source = SkillSource.valueOf(section.getString("kaynak.tur", "").toUpperCase(Locale.ROOT));
            Map<Material, SkillRewardEntry> materialRewards;
            Map<EntityType, SkillRewardEntry> entityRewards;
            SkillRewardEntry defaultEntityReward;
            if (source == SkillSource.ACTION) {
                if (actionRewards.isEmpty()) {
                    throw new IllegalArgumentException("An ACTION skill must define at least one action reward: " + id);
                }
                materialRewards = Map.of();
                entityRewards = Map.of();
                defaultEntityReward = null;
            } else if (source == SkillSource.BLOCK) {
                ConfigurationSection materialsSection = requireSection(section, "kaynak.malzemeler");
                Map<Material, SkillRewardEntry> parsed = new LinkedHashMap<>();
                for (String materialName : materialsSection.getKeys(false)) {
                    parsed.put(parseMaterial(materialName), parseRewardEntry(materialsSection, materialName, id));
                }
                if (parsed.isEmpty()) {
                    throw new IllegalArgumentException("Block skill must define materials: " + id);
                }
                materialRewards = Map.copyOf(parsed);
                entityRewards = Map.of();
                defaultEntityReward = null;
            } else {
                defaultEntityReward = parseRewardEntry(section, "kaynak.varsayilan", id);
                ConfigurationSection entitiesSection = section.getConfigurationSection("kaynak.varliklar");
                Map<EntityType, SkillRewardEntry> parsed = new LinkedHashMap<>();
                if (entitiesSection != null) {
                    for (String entityName : entitiesSection.getKeys(false)) {
                        parsed.put(EntityType.valueOf(entityName.toUpperCase(Locale.ROOT)), parseRewardEntry(entitiesSection, entityName, id));
                    }
                }
                entityRewards = Map.copyOf(parsed);
                materialRewards = Map.of();
            }
            definitions.put(id, new SkillDefinition(id, displayName, icon, model, source, materialRewards, entityRewards, defaultEntityReward, actionRewards, milestones));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("skills.yml must define at least one skill.");
        }
        return new SkillSettings(curve, Map.copyOf(definitions));
    }

    private static final Set<Integer> MILESTONE_TIERS = Set.of(25, 50, 75, 100);

    private Map<Integer, SkillMilestoneEffect> parseMilestones(ConfigurationSection section, String skillId) {
        ConfigurationSection milestonesSection = requireSection(section, "kilometre-taslari");
        Map<Integer, SkillMilestoneEffect> parsed = new LinkedHashMap<>();
        for (String tierKey : milestonesSection.getKeys(false)) {
            int tier = Integer.parseInt(tierKey);
            if (!MILESTONE_TIERS.contains(tier)) {
                throw new IllegalArgumentException("Milestone tier must be one of 25/50/75/100 in skill " + skillId + ": " + tier);
            }
            ConfigurationSection entry = requireSection(milestonesSection, tierKey);
            SkillMilestoneEffectType type;
            try {
                type = SkillMilestoneEffectType.valueOf(entry.getString("tur", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid milestone effect type in skill " + skillId + " tier " + tier, exception);
            }
            double value = entry.getDouble("deger", -1);
            if (value < 0) {
                throw new IllegalArgumentException("Milestone effect value must not be negative in skill " + skillId + " tier " + tier);
            }
            parsed.put(tier, new SkillMilestoneEffect(type, value));
        }
        if (parsed.size() != MILESTONE_TIERS.size()) {
            throw new IllegalArgumentException("Skill must define all 4 milestone tiers (25/50/75/100): " + skillId);
        }
        return Map.copyOf(parsed);
    }

    /**
     * Parses the optional per-skill {@code eylemler} section. {@code actionOwners}
     * is threaded through the whole file so a second skill claiming an action a
     * previous one already declared fails at load time rather than silently
     * losing its rewards to whichever skill happens to be iterated first.
     */
    private Map<SkillAction, SkillActionReward> parseActions(
            ConfigurationSection section, String skillId, Map<SkillAction, String> actionOwners
    ) {
        ConfigurationSection actions = section.getConfigurationSection("eylemler");
        if (actions == null) {
            return Map.of();
        }
        Map<SkillAction, SkillActionReward> parsed = new LinkedHashMap<>();
        for (String rawAction : actions.getKeys(false)) {
            SkillAction action = SkillAction.byId(rawAction)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown skill action '" + rawAction + "' in skill: " + skillId));
            String owner = actionOwners.putIfAbsent(action, skillId);
            if (owner != null) {
                throw new IllegalArgumentException("Skill action '" + rawAction + "' is already claimed by skill: " + owner);
            }
            ConfigurationSection entry = requireSection(actions, rawAction);
            long xp = entry.getLong("xp", 0);
            String displayName = entry.getString("gorunen-ad");
            if (xp <= 0 || displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Invalid action reward '" + rawAction + "' in skill: " + skillId);
            }
            Set<Material> materials = new LinkedHashSet<>();
            for (String rawMaterial : entry.getStringList("malzemeler")) {
                Material material = Material.matchMaterial(rawMaterial);
                if (material == null) {
                    throw new IllegalArgumentException("Invalid action material '" + rawMaterial + "' in skill: " + skillId);
                }
                materials.add(material);
            }
            parsed.put(action, new SkillActionReward(xp, displayName, Set.copyOf(materials)));
        }
        return Map.copyOf(parsed);
    }

    private SkillRewardEntry parseRewardEntry(ConfigurationSection parent, String path, String skillId) {
        ConfigurationSection entry = requireSection(parent, path);
        long xp = entry.getLong("xp", 0);
        String displayName = entry.getString("gorunen-ad");
        if (xp <= 0 || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Invalid reward entry '" + path + "' in skill: " + skillId);
        }
        return new SkillRewardEntry(xp, displayName);
    }

    private ConfigurationSection requireSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    private Material parseMaterial(String raw) {
        Material material = Material.matchMaterial(raw);
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException("Invalid block material: " + raw);
        }
        return material;
    }

    private record SkillKey(UUID playerId, String skillId) {
    }
}
