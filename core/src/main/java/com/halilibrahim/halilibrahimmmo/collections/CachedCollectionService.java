package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
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
import java.util.concurrent.atomic.AtomicReference;

/** YAML-backed collection catalogue plus asynchronous persistent per-player obtained entries. */
public final class CachedCollectionService implements CollectionService {

    private static final String FILE_NAME = "collections.yml";

    private final JavaPlugin plugin;
    private final CollectionRepository repository;
    private final AtomicReference<CollectionSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<CollectionKey, CollectionProgress> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CollectionKey, CompletableFuture<Optional<CollectionEntryAward>>> updateChains = new ConcurrentHashMap<>();

    public CachedCollectionService(JavaPlugin plugin, CollectionRepository repository) {
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
            throw new LifecycleException("collections.yml could not be loaded.", exception);
        }
    }

    @Override
    public CollectionSettings settings() {
        CollectionSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("CollectionService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public Collection<CollectionDefinition> definitions() {
        return settings().definitions().values();
    }

    @Override
    public CompletableFuture<Optional<CollectionProgress>> find(UUID playerId, String collectionId) {
        CollectionDefinition definition = settings().definitions().get(collectionId);
        if (definition == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return load(playerId, definition).thenApply(Optional::of);
    }

    @Override
    public CompletableFuture<Map<String, CollectionProgress>> loadAll(UUID playerId) {
        return repository.findAll(playerId).thenApply(found -> {
            Map<String, CollectionProgress> merged = new LinkedHashMap<>();
            for (CollectionDefinition definition : settings().definitions().values()) {
                CollectionProgress progress = new CollectionProgress(playerId, definition.id(), found.getOrDefault(definition.id(), Set.of()));
                cache.put(new CollectionKey(playerId, definition.id()), progress);
                merged.put(definition.id(), progress);
            }
            return Map.copyOf(merged);
        });
    }

    @Override
    public CompletableFuture<List<CollectionEntryAward>> awardPickup(UUID playerId, Material material) {
        record Match(CollectionDefinition collection, CollectionEntryDefinition entry) {
        }
        List<Match> matches = settings().definitions().values().stream()
                .flatMap(collection -> collection.entries().values().stream()
                        .filter(entry -> entry.item() == material)
                        .map(entry -> new Match(collection, entry)))
                .toList();
        if (matches.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<Optional<CollectionEntryAward>>> futures = matches.stream()
                .map(match -> obtainSerially(playerId, match.collection(), match.entry()))
                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream().map(CompletableFuture::join).flatMap(Optional::stream).toList());
    }

    @Override
    public void evict(UUID playerId) {
        cache.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private CompletableFuture<Optional<CollectionEntryAward>> obtainSerially(UUID playerId, CollectionDefinition collection, CollectionEntryDefinition entry) {
        CollectionKey key = new CollectionKey(playerId, collection.id());
        synchronized (updateChains) {
            CompletableFuture<Optional<CollectionEntryAward>> previous = updateChains.getOrDefault(key, CompletableFuture.completedFuture(Optional.empty()));
            CompletableFuture<Optional<CollectionEntryAward>> next = previous.handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> obtainNow(playerId, collection, entry));
            updateChains.put(key, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(key, next));
            return next;
        }
    }

    private CompletableFuture<Optional<CollectionEntryAward>> obtainNow(UUID playerId, CollectionDefinition collection, CollectionEntryDefinition entry) {
        return load(playerId, collection).thenCompose(progress -> {
            if (progress.obtainedEntryIds().contains(entry.id())) {
                return CompletableFuture.completedFuture(Optional.<CollectionEntryAward>empty());
            }
            Set<String> updatedIds = new LinkedHashSet<>(progress.obtainedEntryIds());
            updatedIds.add(entry.id());
            CollectionProgress updated = new CollectionProgress(playerId, collection.id(), Set.copyOf(updatedIds));
            boolean justCompleted = updated.completed(collection);
            return repository.insertEntry(playerId, collection.id(), entry.id()).thenApply(ignored -> {
                cache.put(new CollectionKey(playerId, collection.id()), updated);
                return Optional.of(new CollectionEntryAward(
                        collection.id(), collection.displayName(),
                        entry.id(), entry.displayName(), entry.rewardExperience(),
                        justCompleted, justCompleted ? collection.completionRewardExperience() : 0
                ));
            });
        });
    }

    private CompletableFuture<CollectionProgress> load(UUID playerId, CollectionDefinition collection) {
        CollectionKey key = new CollectionKey(playerId, collection.id());
        CollectionProgress cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findAll(playerId).thenApply(found -> {
            for (CollectionDefinition other : settings().definitions().values()) {
                cache.putIfAbsent(new CollectionKey(playerId, other.id()), new CollectionProgress(playerId, other.id(), found.getOrDefault(other.id(), Set.of())));
            }
            return cache.computeIfAbsent(key, ignored -> new CollectionProgress(playerId, collection.id(), Set.of()));
        });
    }

    private CollectionSettings parse(YamlConfiguration yaml) {
        ConfigurationSection collections = requiredSection(yaml, "koleksiyonlar");
        Map<String, CollectionDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : collections.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate collection id: " + rawId);
            }
            ConfigurationSection section = requiredSection(collections, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Collection display name required: " + id);
            }
            long completionReward = section.getLong("tamamlama-xp", 0);
            if (completionReward <= 0) {
                throw new IllegalArgumentException("Collection completion XP must be positive: " + id);
            }
            ConfigurationSection entries = requiredSection(section, "ogeler");
            Map<String, CollectionEntryDefinition> entryDefinitions = new LinkedHashMap<>();
            for (String rawEntryId : entries.getKeys(false)) {
                String entryId = rawEntryId.toLowerCase(Locale.ROOT);
                ConfigurationSection entry = requiredSection(entries, rawEntryId);
                String entryName = entry.getString("gorunen-ad");
                Material item = Material.matchMaterial(entry.getString("esya", ""));
                long entryReward = entry.getLong("xp", 0);
                if (!entryId.matches("[a-z0-9_-]+") || entryDefinitions.containsKey(entryId)
                        || entryName == null || entryName.isBlank() || item == null || entryReward <= 0) {
                    throw new IllegalArgumentException("Invalid collection entry: " + rawEntryId);
                }
                entryDefinitions.put(entryId, new CollectionEntryDefinition(entryId, entryName, item, entryReward));
            }
            if (entryDefinitions.isEmpty()) {
                throw new IllegalArgumentException("Collection must define entries: " + id);
            }
            definitions.put(id, new CollectionDefinition(id, displayName, Map.copyOf(entryDefinitions), completionReward));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("collections.yml must define at least one collection.");
        }
        return new CollectionSettings(Map.copyOf(definitions));
    }

    private ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

    private record CollectionKey(UUID playerId, String collectionId) {
    }
}
