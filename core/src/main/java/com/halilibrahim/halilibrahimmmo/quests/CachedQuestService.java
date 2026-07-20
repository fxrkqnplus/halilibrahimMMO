package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** YAML-backed quest catalogue plus asynchronous persistent per-player progress. */
public final class CachedQuestService implements QuestService {

    private static final String FILE_NAME = "quests.yml";

    private final JavaPlugin plugin;
    private final QuestRepository repository;
    private final AtomicReference<QuestSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<QuestKey, QuestProgress> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<QuestKey, CompletableFuture<Optional<QuestProgress>>> updateChains = new ConcurrentHashMap<>();

    public CachedQuestService(JavaPlugin plugin, QuestRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
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
            throw new LifecycleException("quests.yml could not be loaded.", exception);
        }
    }

    @Override
    public QuestSettings settings() {
        QuestSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("QuestService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public Collection<QuestDefinition> definitions() {
        return settings().definitions().values();
    }

    @Override
    public CompletableFuture<Optional<QuestProgress>> find(UUID playerId, String questId) {
        QuestDefinition definition = settings().definitions().get(questId);
        if (definition == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return load(playerId, definition).thenApply(Optional::of);
    }

    @Override
    public CompletableFuture<Map<String, QuestProgress>> loadAll(UUID playerId) {
        return repository.findAll(playerId).thenApply(found -> {
            Map<String, QuestProgress> merged = new LinkedHashMap<>();
            for (QuestDefinition definition : settings().definitions().values()) {
                QuestProgress progress = found.getOrDefault(definition.id(), freshProgress(playerId, definition));
                cache.put(new QuestKey(playerId, definition.id()), progress);
                merged.put(definition.id(), progress);
            }
            return Map.copyOf(merged);
        });
    }

    @Override
    public CompletableFuture<List<QuestProgress>> awardBlock(UUID playerId, Material material) {
        return awardMatching(playerId, QuestObjectiveType.BLOCK_BREAK, definition -> definition.materials().contains(material));
    }

    @Override
    public CompletableFuture<List<QuestProgress>> awardMob(UUID playerId, EntityType entityType) {
        return awardMatching(playerId, QuestObjectiveType.MOB_KILL,
                definition -> definition.entityTypes().isEmpty() || definition.entityTypes().contains(entityType));
    }

    @Override
    public void evict(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private CompletableFuture<List<QuestProgress>> awardMatching(UUID playerId, QuestObjectiveType objectiveType, Predicate<QuestDefinition> matches) {
        List<QuestDefinition> matching = settings().definitions().values().stream()
                .filter(definition -> definition.objectiveType() == objectiveType)
                .filter(matches)
                .toList();
        if (matching.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<Optional<QuestProgress>>> futures = matching.stream()
                .map(definition -> incrementSerially(playerId, definition))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
    }

    private CompletableFuture<Optional<QuestProgress>> incrementSerially(UUID playerId, QuestDefinition definition) {
        QuestKey key = new QuestKey(playerId, definition.id());
        synchronized (updateChains) {
            CompletableFuture<Optional<QuestProgress>> previous = updateChains.getOrDefault(key, CompletableFuture.completedFuture(Optional.empty()));
            CompletableFuture<Optional<QuestProgress>> next = previous.handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> incrementNow(playerId, definition));
            updateChains.put(key, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(key, next));
            return next;
        }
    }

    private CompletableFuture<Optional<QuestProgress>> incrementNow(UUID playerId, QuestDefinition definition) {
        return load(playerId, definition).thenCompose(progress -> {
            long now = System.currentTimeMillis();
            QuestProgress current = QuestResetRules.isExpired(definition.type(), progress.periodEndMillis(), now)
                    ? freshProgress(playerId, definition)
                    : progress;
            if (current.completed()) {
                return CompletableFuture.completedFuture(Optional.<QuestProgress>empty());
            }
            int nextProgress = Math.min(definition.target(), current.progress() + 1);
            boolean justCompleted = nextProgress >= definition.target();
            QuestProgress updated = new QuestProgress(playerId, definition.id(), nextProgress, justCompleted, current.periodEndMillis());
            return repository.save(updated).thenApply(ignored -> {
                cache.put(new QuestKey(playerId, definition.id()), updated);
                return justCompleted ? Optional.of(updated) : Optional.<QuestProgress>empty();
            });
        });
    }

    private CompletableFuture<QuestProgress> load(UUID playerId, QuestDefinition definition) {
        QuestKey key = new QuestKey(playerId, definition.id());
        QuestProgress cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findAll(playerId).thenApply(found -> {
            for (QuestDefinition other : settings().definitions().values()) {
                cache.putIfAbsent(new QuestKey(playerId, other.id()), found.getOrDefault(other.id(), freshProgress(playerId, other)));
            }
            return cache.computeIfAbsent(key, ignored -> freshProgress(playerId, definition));
        });
    }

    private QuestProgress freshProgress(UUID playerId, QuestDefinition definition) {
        long now = System.currentTimeMillis();
        return new QuestProgress(playerId, definition.id(), 0, false, QuestResetRules.nextPeriodEnd(definition.type(), now));
    }

    private QuestSettings parse(YamlConfiguration yaml) {
        ConfigurationSection quests = requiredSection(yaml, "gorevler");
        Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : quests.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate quest id: " + rawId);
            }
            ConfigurationSection section = requiredSection(quests, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Quest display name required: " + id);
            }
            QuestType type = QuestType.valueOf(section.getString("tur", "").toUpperCase(Locale.ROOT));
            ConfigurationSection objective = requiredSection(section, "hedef");
            QuestObjectiveType objectiveType = QuestObjectiveType.valueOf(objective.getString("tur", "").toUpperCase(Locale.ROOT));
            Set<Material> materials = objectiveType == QuestObjectiveType.BLOCK_BREAK
                    ? objective.getStringList("malzemeler").stream().map(this::parseBlockMaterial).collect(Collectors.toUnmodifiableSet())
                    : Set.of();
            if (objectiveType == QuestObjectiveType.BLOCK_BREAK && materials.isEmpty()) {
                throw new IllegalArgumentException("Block quest must define materials: " + id);
            }
            Set<EntityType> entityTypes = objectiveType == QuestObjectiveType.MOB_KILL
                    ? objective.getStringList("varliklar").stream().map(this::parseEntityType).collect(Collectors.toUnmodifiableSet())
                    : Set.of();
            int target = section.getInt("sayi", 0);
            long reward = section.getLong("xp", 0);
            if (target < 1 || reward <= 0) {
                throw new IllegalArgumentException("Quest target and XP reward must be positive: " + id);
            }
            definitions.put(id, new QuestDefinition(id, displayName, type, objectiveType, materials, entityTypes, target, reward));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("quests.yml must define at least one quest.");
        }
        return new QuestSettings(Map.copyOf(definitions));
    }

    private ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    private Material parseBlockMaterial(String raw) {
        Material material = Material.matchMaterial(raw);
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException("Invalid block material: " + raw);
        }
        return material;
    }

    private EntityType parseEntityType(String raw) {
        try {
            return EntityType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid entity type: " + raw, exception);
        }
    }

    private record QuestKey(UUID playerId, String questId) {
    }
}
