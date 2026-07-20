package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** YAML-backed, write-through cache for all configured player skills. */
public final class CachedSkillService implements SkillService {

    private static final String FILE_NAME = "skills.yml";

    private final JavaPlugin plugin;
    private final SkillRepository repository;
    private final AtomicReference<SkillSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<SkillKey, SkillProgress> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SkillKey, CompletableFuture<SkillProgress>> updateChains = new ConcurrentHashMap<>();

    public CachedSkillService(JavaPlugin plugin, SkillRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
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
    public CompletableFuture<Optional<SkillProgress>> awardBlock(UUID playerId, Material material) {
        return awardMatching(playerId, SkillSource.BLOCK, material);
    }

    @Override
    public CompletableFuture<Optional<SkillProgress>> awardMob(UUID playerId) {
        return awardMatching(playerId, SkillSource.MOB, null);
    }

    @Override
    public Optional<SkillProgress> cached(UUID playerId, String skillId) {
        return Optional.ofNullable(cache.get(new SkillKey(playerId, skillId)));
    }

    @Override
    public void evict(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private CompletableFuture<Optional<SkillProgress>> awardMatching(UUID playerId, SkillSource source, Material material) {
        Optional<SkillDefinition> definition = settings().definitions().values().stream()
                .filter(candidate -> candidate.source() == source)
                .filter(candidate -> source != SkillSource.BLOCK || candidate.materials().contains(material))
                .findFirst();
        if (definition.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        SkillDefinition skill = definition.get();
        SkillKey key = new SkillKey(playerId, skill.id());
        synchronized (updateChains) {
            CompletableFuture<SkillProgress> previous = updateChains.get(key);
            CompletableFuture<SkillProgress> base = previous == null ? load(playerId, skill.id()) : previous;
            CompletableFuture<SkillProgress> next = base.thenApply(progress -> settings().curve().award(progress, skill.reward()))
                    .thenCompose(progress -> repository.save(progress).thenApply(ignored -> progress))
                    .thenApply(progress -> {
                        cache.put(key, progress);
                        return progress;
                    });
            updateChains.put(key, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(key, next));
            return next.thenApply(Optional::of);
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
            SkillSource source = SkillSource.valueOf(section.getString("kaynak.tur", "").toUpperCase(Locale.ROOT));
            long reward = section.getLong("xp", 0);
            if (reward <= 0) {
                throw new IllegalArgumentException("Skill XP reward must be positive: " + id);
            }
            Set<Material> materials = source == SkillSource.BLOCK
                    ? section.getStringList("kaynak.malzemeler").stream().map(this::parseMaterial).collect(java.util.stream.Collectors.toUnmodifiableSet())
                    : Set.of();
            if (source == SkillSource.BLOCK && materials.isEmpty()) {
                throw new IllegalArgumentException("Block skill must define materials: " + id);
            }
            definitions.put(id, new SkillDefinition(id, displayName, source, materials, reward));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("skills.yml must define at least one skill.");
        }
        return new SkillSettings(curve, Map.copyOf(definitions));
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
