package com.halilibrahim.halilibrahimmmo.prestige;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Join-warmed cache over {@link PrestigeRewardRepository}, plus the one effect
 * that has nowhere else to live.
 *
 * <p>Three of the four rewards are consumed by code that already sums bonuses
 * from several sources - maximum energy in the plugin bootstrap, creature
 * damage in {@code ClassListener}, skill XP in the synergy multiplier - so this
 * service only has to answer with a number. {@link PrestigeRewardType#CAN} is
 * the exception: nothing else adds maximum health except the class service's
 * own modifier, and reaching into that would make {@code classes} depend on
 * {@code prestige}, which already depends on {@code classes}. It therefore gets
 * <strong>its own stable-keyed attribute modifier</strong>, applied
 * independently and removed-then-reapplied idempotently - the same trick
 * {@code YamlClassService} and {@code CachedSkillService} each use for their own
 * health bonuses, and the reason all three can coexist on one player.</p>
 */
public final class CachedPrestigeRewardService implements PrestigeRewardService {

    private final JavaPlugin plugin;
    private final PrestigeRewardRepository repository;
    private final Supplier<Map<PrestigeRewardType, Double>> valueSupplier;
    private final ConcurrentHashMap<UUID, Map<PrestigeRewardType, Integer>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> updateChains = new ConcurrentHashMap<>();
    /** How many crates this player has already taken - the physical half's whole state. */
    private final ConcurrentHashMap<UUID, Integer> physicalCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<Boolean>> physicalChains = new ConcurrentHashMap<>();
    private final NamespacedKey healthModifierKey;

    public CachedPrestigeRewardService(
            JavaPlugin plugin, PrestigeRewardRepository repository, Supplier<Map<PrestigeRewardType, Double>> valueSupplier
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.valueSupplier = valueSupplier;
        this.healthModifierKey = new NamespacedKey(plugin, "prestij-odul-can");
    }

    @Override
    public void enable() {
        // Nothing to read at startup: every player's picks are loaded on join.
    }

    @Override
    public void disable() {
        cache.clear();
        updateChains.clear();
        physicalCache.clear();
        physicalChains.clear();
    }

    @Override
    public Map<PrestigeRewardType, Double> values() {
        return valueSupplier.get();
    }

    @Override
    public CompletableFuture<Void> load(UUID playerId) {
        return repository.findAll(playerId)
                .thenAccept(found -> cache.put(playerId, found))
                .thenCompose(ignored -> repository.findPhysicalTaken(playerId))
                .thenAccept(taken -> physicalCache.put(playerId, Math.max(0, taken)));
    }

    @Override
    public int pendingPhysicalChoices(UUID playerId, int prestige) {
        return Math.max(0, prestige - physicalCache.getOrDefault(playerId, 0));
    }

    /**
     * Rides its own chain rather than sharing the permanent reward's, because
     * the two are independent counters and a player is expected to spend one of
     * each in quick succession - serialising them against each other would only
     * make the second click wait for the first for no benefit.
     */
    @Override
    public CompletableFuture<Boolean> claimPhysical(UUID playerId, int prestige) {
        synchronized (physicalChains) {
            CompletableFuture<Boolean> previous =
                    physicalChains.getOrDefault(playerId, CompletableFuture.completedFuture(false));
            CompletableFuture<Boolean> next = previous.handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> {
                        if (pendingPhysicalChoices(playerId, prestige) <= 0) {
                            return CompletableFuture.completedFuture(false);
                        }
                        int updated = physicalCache.getOrDefault(playerId, 0) + 1;
                        return repository.savePhysicalTaken(playerId, updated).thenApply(saved -> {
                            physicalCache.put(playerId, updated);
                            return true;
                        });
                    });
            physicalChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> physicalChains.remove(playerId, next));
            return next;
        }
    }

    @Override
    public Map<PrestigeRewardType, Integer> cached(UUID playerId) {
        return cache.getOrDefault(playerId, Map.of());
    }

    @Override
    public int pendingChoices(UUID playerId, int prestige) {
        int spent = cached(playerId).values().stream().mapToInt(Integer::intValue).sum();
        return Math.max(0, prestige - spent);
    }

    /**
     * Serialised per player, like every other mutation in this codebase: two
     * clicks landing in the same tick would otherwise both read "one choice
     * left" and both write.
     */
    @Override
    public CompletableFuture<Boolean> choose(UUID playerId, PrestigeRewardType type, int prestige) {
        synchronized (updateChains) {
            CompletableFuture<Boolean> previous = updateChains.getOrDefault(playerId, CompletableFuture.completedFuture(false));
            CompletableFuture<Boolean> next = previous.handle((ignoredValue, ignoredThrowable) -> null)
                    .thenCompose(ignored -> chooseNow(playerId, type, prestige));
            updateChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(playerId, next));
            return next;
        }
    }

    private CompletableFuture<Boolean> chooseNow(UUID playerId, PrestigeRewardType type, int prestige) {
        if (pendingChoices(playerId, prestige) <= 0) {
            return CompletableFuture.completedFuture(false);
        }
        int updated = cached(playerId).getOrDefault(type, 0) + 1;
        return repository.save(playerId, type, updated).thenApply(ignored -> {
            Map<PrestigeRewardType, Integer> merged = new EnumMap<>(PrestigeRewardType.class);
            merged.putAll(cached(playerId));
            merged.put(type, updated);
            cache.put(playerId, Map.copyOf(merged));
            return true;
        });
    }

    @Override
    public double healthBonus(UUID playerId) {
        return amountOf(playerId, PrestigeRewardType.CAN);
    }

    @Override
    public double damageBonus(UUID playerId) {
        return amountOf(playerId, PrestigeRewardType.HASAR);
    }

    @Override
    public int energyBonus(UUID playerId) {
        return (int) Math.round(amountOf(playerId, PrestigeRewardType.ENERJI));
    }

    @Override
    public double experienceMultiplier(UUID playerId) {
        return 1.0D + amountOf(playerId, PrestigeRewardType.BILGELIK);
    }

    private double amountOf(UUID playerId, PrestigeRewardType type) {
        return cached(playerId).getOrDefault(type, 0) * values().getOrDefault(type, 0.0D);
    }

    /**
     * Applies (or reapplies) the health reward. Must be called from the
     * player's own region, and is called again after a respawn for the same
     * reason class and milestone health bonuses are: the server resets a
     * player's attributes after the respawn event returns.
     */
    @Override
    public void applyPlayerEffects(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(healthModifierKey);
        double bonus = healthBonus(player.getUniqueId());
        if (bonus != 0) {
            attribute.addModifier(new AttributeModifier(healthModifierKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
        player.setHealth(Math.min(player.getHealth(), attribute.getValue()));
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
        physicalCache.remove(playerId);
    }
}
