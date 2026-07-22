package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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
    private final ConcurrentHashMap<AssignmentKey, QuestAssignment> assignmentCache = new ConcurrentHashMap<>();

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
        assignmentCache.clear();
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
        return ensureAssignment(playerId, QuestType.DAILY).thenCompose(daily ->
                ensureAssignment(playerId, QuestType.WEEKLY).thenCompose(weekly ->
                        repository.findAll(playerId).thenApply(found -> {
                            Map<String, QuestProgress> merged = new LinkedHashMap<>();
                            for (QuestDefinition definition : settings().definitions().values()) {
                                if (!isRelevant(definition, daily, weekly)) {
                                    continue;
                                }
                                QuestProgress progress = found.getOrDefault(definition.id(), freshProgress(playerId, definition));
                                cache.put(new QuestKey(playerId, definition.id()), progress);
                                merged.put(definition.id(), progress);
                            }
                            return Map.copyOf(merged);
                        })));
    }

    /** {@code true} for every PERMANENT quest, or a DAILY/WEEKLY quest currently assigned to this player. */
    private boolean isRelevant(QuestDefinition definition, QuestAssignment dailyAssignment, QuestAssignment weeklyAssignment) {
        return switch (definition.type()) {
            case PERMANENT -> true;
            case DAILY -> dailyAssignment.questIds().contains(definition.id());
            case WEEKLY -> weeklyAssignment.questIds().contains(definition.id());
        };
    }

    /**
     * Loads (assigning fresh, randomly-picked quest ids from the pool if
     * missing or expired) this player's current DAILY/WEEKLY assignment.
     * Never called for PERMANENT - those are never pooled.
     */
    private CompletableFuture<QuestAssignment> ensureAssignment(UUID playerId, QuestType type) {
        AssignmentKey key = new AssignmentKey(playerId, type);
        long now = System.currentTimeMillis();
        QuestAssignment cached = assignmentCache.get(key);
        if (cached != null && !QuestResetRules.isExpired(type, cached.periodEndMillis(), now)) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findAssignment(playerId, type).thenCompose(found -> {
            if (found.isPresent() && !QuestResetRules.isExpired(type, found.get().periodEndMillis(), now)) {
                assignmentCache.put(key, found.get());
                return CompletableFuture.completedFuture(found.get());
            }
            QuestAssignment fresh = randomAssignment(type, now);
            return repository.saveAssignment(playerId, type, fresh).thenApply(ignored -> {
                assignmentCache.put(key, fresh);
                return fresh;
            });
        });
    }

    private QuestAssignment randomAssignment(QuestType type, long now) {
        List<String> pool = new ArrayList<>(settings().definitions().values().stream()
                .filter(definition -> definition.type() == type)
                .map(QuestDefinition::id)
                .toList());
        Collections.shuffle(pool, ThreadLocalRandom.current());
        int count = Math.min(pool.size(), settings().assignedCount(type));
        Set<String> chosen = new LinkedHashSet<>(pool.subList(0, count));
        return new QuestAssignment(Set.copyOf(chosen), QuestResetRules.nextPeriodEnd(type, now));
    }

    private record AssignmentKey(UUID playerId, QuestType type) {
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
    public Map<String, QuestProgress> cachedAll(UUID playerId) {
        Map<String, QuestProgress> result = new LinkedHashMap<>();
        cache.forEach((key, progress) -> {
            if (key.playerId().equals(playerId)) {
                result.put(key.questId(), progress);
            }
        });
        return Map.copyOf(result);
    }

    @Override
    public void evict(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
        assignmentCache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private CompletableFuture<List<QuestProgress>> awardMatching(UUID playerId, QuestObjectiveType objectiveType, Predicate<QuestDefinition> matches) {
        List<QuestDefinition> candidates = settings().definitions().values().stream()
                .filter(definition -> definition.objectiveType() == objectiveType)
                .filter(matches)
                .toList();
        if (candidates.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return ensureAssignment(playerId, QuestType.DAILY).thenCompose(daily ->
                ensureAssignment(playerId, QuestType.WEEKLY).thenCompose(weekly -> {
                    List<QuestDefinition> matching = candidates.stream()
                            .filter(definition -> isRelevant(definition, daily, weekly))
                            .toList();
                    if (matching.isEmpty()) {
                        return CompletableFuture.completedFuture(List.<QuestProgress>of());
                    }
                    List<CompletableFuture<Optional<QuestProgress>>> futures = matching.stream()
                            .map(definition -> incrementSerially(playerId, definition))
                            .toList();
                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(ignored -> futures.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
                }));
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
                // Only ever resurrect an unassigned DAILY/WEEKLY pool quest into the
                // cache for the one specifically requested here - backfilling the
                // whole pool would defeat loadAll(...)'s assignment filtering.
                if (other.type() != QuestType.PERMANENT && !other.id().equals(definition.id())) {
                    continue;
                }
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
        int dailyAssignedCount = yaml.getInt("gunluk-atanan-sayisi", 3);
        int weeklyAssignedCount = yaml.getInt("haftalik-atanan-sayisi", 2);
        if (dailyAssignedCount < 1 || weeklyAssignedCount < 1) {
            throw new IllegalArgumentException("Assigned daily/weekly quest counts must be positive.");
        }
        return new QuestSettings(Map.copyOf(definitions), dailyAssignedCount, weeklyAssignedCount);
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
