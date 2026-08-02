package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionParser;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditions;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockContextLoader;
import com.halilibrahim.halilibrahimmmo.resourcepack.ItemModels;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import java.util.function.Supplier;

/** YAML-backed title catalogue plus asynchronous persistent unlock/equip state. */
public final class CachedTitleService implements TitleService {

    private static final String FILE_NAME = "titles.yml";

    private final JavaPlugin plugin;
    private final TitleRepository repository;
    private final UnlockContextLoader contextLoader;
    private final AtomicReference<TitleSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, Set<String>> unlockedCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> equippedIdCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> equippedDisplayNameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TitleChatStyle> equippedStyleCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateChains = new ConcurrentHashMap<>();

    public CachedTitleService(JavaPlugin plugin, TitleRepository repository, UnlockContextLoader contextLoader) {
        this.plugin = plugin;
        this.repository = repository;
        this.contextLoader = contextLoader;
    }

    @Override
    public void enable() {
        reload();
    }

    @Override
    public void disable() {
        unlockedCache.clear();
        equippedIdCache.clear();
        equippedDisplayNameCache.clear();
        equippedStyleCache.clear();
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
            throw new LifecycleException("titles.yml could not be loaded.", exception);
        }
    }

    @Override
    public TitleSettings settings() {
        TitleSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("TitleService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public Collection<TitleDefinition> definitions() {
        return settings().definitions().values();
    }

    @Override
    public CompletableFuture<Set<String>> unlockedIds(UUID playerId) {
        Set<String> cached = unlockedCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findUnlockedIds(playerId).thenApply(ids -> {
            unlockedCache.put(playerId, ids);
            return ids;
        });
    }

    @Override
    public CompletableFuture<Optional<TitleDefinition>> findEquipped(UUID playerId) {
        String cachedId = equippedIdCache.get(playerId);
        if (cachedId != null) {
            return CompletableFuture.completedFuture(resolveEquipped(cachedId));
        }
        return repository.findEquipped(playerId).thenApply(id -> {
            cacheEquipped(playerId, id);
            return resolveEquipped(id);
        });
    }

    @Override
    public CompletableFuture<TitleEquipResult> equip(UUID playerId, String playerName, String rawTitleId) {
        String titleId = rawTitleId.toLowerCase(Locale.ROOT);
        if (!settings().definitions().containsKey(titleId)) {
            return CompletableFuture.completedFuture(new TitleEquipResult(TitleEquipResult.Status.UNKNOWN_TITLE, titleId));
        }
        return serial(playerId, () -> equipNow(playerId, titleId));
    }

    private CompletableFuture<TitleEquipResult> equipNow(UUID playerId, String titleId) {
        return unlockedIds(playerId).thenCompose(unlocked -> {
            if (!unlocked.contains(titleId)) {
                return CompletableFuture.completedFuture(new TitleEquipResult(TitleEquipResult.Status.NOT_UNLOCKED, titleId));
            }
            return findEquipped(playerId).thenCompose(equipped -> {
                if (equipped.isPresent() && equipped.get().id().equals(titleId)) {
                    return CompletableFuture.completedFuture(new TitleEquipResult(TitleEquipResult.Status.ALREADY_EQUIPPED, titleId));
                }
                return repository.saveEquipped(playerId, titleId).thenApply(ignored -> {
                    cacheEquipped(playerId, titleId);
                    return new TitleEquipResult(TitleEquipResult.Status.EQUIPPED, titleId);
                });
            });
        });
    }

    @Override
    public CompletableFuture<List<TitleDefinition>> evaluate(UUID playerId, String playerName) {
        return serial(playerId, () -> evaluateNow(playerId, playerName));
    }

    private CompletableFuture<List<TitleDefinition>> evaluateNow(UUID playerId, String playerName) {
        return unlockedIds(playerId).thenCompose(unlocked -> {
            List<TitleDefinition> candidates = definitions().stream()
                    .filter(definition -> !unlocked.contains(definition.id()))
                    .toList();
            if (candidates.isEmpty()) {
                return CompletableFuture.completedFuture(List.<TitleDefinition>of());
            }
            return contextLoader.load(playerId, playerName).thenCompose(context -> {
                List<TitleDefinition> satisfied = candidates.stream()
                        .filter(definition -> UnlockConditions.isSatisfied(definition.condition(), context))
                        .toList();
                if (satisfied.isEmpty()) {
                    return CompletableFuture.completedFuture(List.<TitleDefinition>of());
                }
                CompletableFuture<?>[] inserts = satisfied.stream()
                        .map(definition -> repository.insertUnlock(playerId, definition.id()))
                        .toArray(CompletableFuture[]::new);
                return CompletableFuture.allOf(inserts).thenApply(ignored -> {
                    Set<String> updated = new LinkedHashSet<>(unlocked);
                    satisfied.forEach(definition -> updated.add(definition.id()));
                    unlockedCache.put(playerId, Set.copyOf(updated));
                    return satisfied;
                });
            });
        });
    }

    @Override
    public Optional<String> equippedDisplayName(UUID playerId) {
        return Optional.ofNullable(equippedDisplayNameCache.get(playerId));
    }

    @Override
    public Optional<TitleChatStyle> equippedStyle(UUID playerId) {
        return Optional.ofNullable(equippedStyleCache.get(playerId));
    }

    @Override
    public void evict(UUID playerId) {
        unlockedCache.remove(playerId);
        equippedIdCache.remove(playerId);
        equippedDisplayNameCache.remove(playerId);
        equippedStyleCache.remove(playerId);
    }

    private Optional<TitleDefinition> resolveEquipped(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(settings().definitions().get(id));
    }

    private void cacheEquipped(UUID playerId, String id) {
        equippedIdCache.put(playerId, id == null ? "" : id);
        TitleDefinition definition = resolveEquipped(id).orElse(null);
        if (definition == null) {
            equippedDisplayNameCache.remove(playerId);
            equippedStyleCache.remove(playerId);
        } else {
            equippedDisplayNameCache.put(playerId, definition.displayName());
            equippedStyleCache.put(playerId, definition.chatStyle());
        }
    }

    private TitleSettings parse(YamlConfiguration yaml) {
        ConfigurationSection titles = requiredSection(yaml, "unvanlar");
        Map<String, TitleDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : titles.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate title id: " + rawId);
            }
            ConfigurationSection section = requiredSection(titles, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Title display name required: " + id);
            }
            Material icon = Material.matchMaterial(section.getString("esya", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Invalid title icon material: " + id);
            }
            // Optional: names a texture the server resource pack ships.
            // Absent leaves the icon exactly as it was, which is what lets a
            // server keep its unregenerated YAML and still run this version.
            NamespacedKey model = ItemModels.parse(section);
            UnlockCondition condition = UnlockConditionParser.parse(requiredSection(section, "kosul"), id);
            TitleChatStyle chatStyle = TitleChatStyle.valueOf(section.getString("sohbet-stili", "").toUpperCase(Locale.ROOT));
            definitions.put(id, new TitleDefinition(id, displayName, icon, model, condition, chatStyle));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("titles.yml must define at least one title.");
        }
        return new TitleSettings(Map.copyOf(definitions));
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing YAML section: " + path);
        }
        return section;
    }

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
