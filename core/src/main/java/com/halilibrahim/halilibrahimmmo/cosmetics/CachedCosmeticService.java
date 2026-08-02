package com.halilibrahim.halilibrahimmmo.cosmetics;

import com.halilibrahim.halilibrahimmmo.core.LifecycleException;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditionParser;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockConditions;
import com.halilibrahim.halilibrahimmmo.unlock.UnlockContextLoader;
import com.halilibrahim.halilibrahimmmo.resourcepack.ItemModels;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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

/** YAML-backed cosmetic catalogue plus asynchronous persistent unlock/equip state. */
public final class CachedCosmeticService implements CosmeticService {

    private static final String FILE_NAME = "cosmetics.yml";
    private static final int PARTICLE_COUNT = 30;
    private static final double PARTICLE_SPREAD = 0.5D;
    private static final double PARTICLE_SPEED = 0.01D;

    private final JavaPlugin plugin;
    private final CosmeticRepository repository;
    private final UnlockContextLoader contextLoader;
    private final AtomicReference<CosmeticSettings> settings = new AtomicReference<>();
    private final ConcurrentHashMap<UUID, Set<String>> unlockedCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> equippedIdCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> equippedDisplayNameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> updateChains = new ConcurrentHashMap<>();

    public CachedCosmeticService(JavaPlugin plugin, CosmeticRepository repository, UnlockContextLoader contextLoader) {
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
            throw new LifecycleException("cosmetics.yml could not be loaded.", exception);
        }
    }

    @Override
    public CosmeticSettings settings() {
        CosmeticSettings value = settings.get();
        if (value == null) {
            throw new IllegalStateException("CosmeticService.enable() has not run yet.");
        }
        return value;
    }

    @Override
    public Collection<CosmeticDefinition> definitions() {
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
    public CompletableFuture<Optional<CosmeticDefinition>> findEquipped(UUID playerId) {
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
    public CompletableFuture<CosmeticEquipResult> equip(UUID playerId, String playerName, String rawCosmeticId) {
        String cosmeticId = rawCosmeticId.toLowerCase(Locale.ROOT);
        if (!settings().definitions().containsKey(cosmeticId)) {
            return CompletableFuture.completedFuture(new CosmeticEquipResult(CosmeticEquipResult.Status.UNKNOWN_COSMETIC, cosmeticId));
        }
        return serial(playerId, () -> equipNow(playerId, cosmeticId));
    }

    private CompletableFuture<CosmeticEquipResult> equipNow(UUID playerId, String cosmeticId) {
        return unlockedIds(playerId).thenCompose(unlocked -> {
            if (!unlocked.contains(cosmeticId)) {
                return CompletableFuture.completedFuture(new CosmeticEquipResult(CosmeticEquipResult.Status.NOT_UNLOCKED, cosmeticId));
            }
            return findEquipped(playerId).thenCompose(equipped -> {
                if (equipped.isPresent() && equipped.get().id().equals(cosmeticId)) {
                    return CompletableFuture.completedFuture(new CosmeticEquipResult(CosmeticEquipResult.Status.ALREADY_EQUIPPED, cosmeticId));
                }
                return repository.saveEquipped(playerId, cosmeticId).thenApply(ignored -> {
                    cacheEquipped(playerId, cosmeticId);
                    return new CosmeticEquipResult(CosmeticEquipResult.Status.EQUIPPED, cosmeticId);
                });
            });
        });
    }

    @Override
    public CompletableFuture<List<CosmeticDefinition>> evaluate(UUID playerId, String playerName) {
        return serial(playerId, () -> evaluateNow(playerId, playerName));
    }

    private CompletableFuture<List<CosmeticDefinition>> evaluateNow(UUID playerId, String playerName) {
        return unlockedIds(playerId).thenCompose(unlocked -> {
            List<CosmeticDefinition> candidates = definitions().stream()
                    .filter(definition -> !unlocked.contains(definition.id()))
                    .toList();
            if (candidates.isEmpty()) {
                return CompletableFuture.completedFuture(List.<CosmeticDefinition>of());
            }
            return contextLoader.load(playerId, playerName).thenCompose(context -> {
                List<CosmeticDefinition> satisfied = candidates.stream()
                        .filter(definition -> UnlockConditions.isSatisfied(definition.condition(), context))
                        .toList();
                if (satisfied.isEmpty()) {
                    return CompletableFuture.completedFuture(List.<CosmeticDefinition>of());
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
    public void celebrate(Player player, CelebrationMoment moment) {
        String equippedId = equippedIdCache.get(player.getUniqueId());
        if (equippedId == null || equippedId.isEmpty()) {
            return;
        }
        CosmeticDefinition definition = settings().definitions().get(equippedId);
        if (definition == null) {
            return;
        }
        player.getWorld().spawnParticle(definition.effect(), player.getLocation().add(0, 1, 0),
                PARTICLE_COUNT, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPEED);
    }

    @Override
    public Optional<String> equippedDisplayName(UUID playerId) {
        return Optional.ofNullable(equippedDisplayNameCache.get(playerId));
    }

    @Override
    public void evict(UUID playerId) {
        unlockedCache.remove(playerId);
        equippedIdCache.remove(playerId);
        equippedDisplayNameCache.remove(playerId);
    }

    private Optional<CosmeticDefinition> resolveEquipped(String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(settings().definitions().get(id));
    }

    private void cacheEquipped(UUID playerId, String id) {
        equippedIdCache.put(playerId, id == null ? "" : id);
        CosmeticDefinition definition = resolveEquipped(id).orElse(null);
        if (definition == null) {
            equippedDisplayNameCache.remove(playerId);
        } else {
            equippedDisplayNameCache.put(playerId, definition.displayName());
        }
    }

    private CosmeticSettings parse(YamlConfiguration yaml) {
        ConfigurationSection cosmetics = requiredSection(yaml, "kozmetikler");
        Map<String, CosmeticDefinition> definitions = new LinkedHashMap<>();
        for (String rawId : cosmetics.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+") || definitions.containsKey(id)) {
                throw new IllegalArgumentException("Invalid or duplicate cosmetic id: " + rawId);
            }
            ConfigurationSection section = requiredSection(cosmetics, rawId);
            String displayName = section.getString("gorunen-ad");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Cosmetic display name required: " + id);
            }
            Material icon = Material.matchMaterial(section.getString("esya", ""));
            if (icon == null) {
                throw new IllegalArgumentException("Invalid cosmetic icon material: " + id);
            }
            // Optional: names a texture the server resource pack ships.
            // Absent leaves the icon exactly as it was, which is what lets a
            // server keep its unregenerated YAML and still run this version.
            NamespacedKey model = ItemModels.parse(section);
            Particle effect = Particle.valueOf(section.getString("efekt", "").toUpperCase(Locale.ROOT));
            UnlockCondition condition = UnlockConditionParser.parse(requiredSection(section, "kosul"), id);
            definitions.put(id, new CosmeticDefinition(id, displayName, icon, model, effect, condition));
        }
        if (definitions.isEmpty()) {
            throw new IllegalArgumentException("cosmetics.yml must define at least one cosmetic.");
        }
        return new CosmeticSettings(Map.copyOf(definitions));
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
