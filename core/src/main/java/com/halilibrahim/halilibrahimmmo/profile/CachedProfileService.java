package com.halilibrahim.halilibrahimmmo.profile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Owns the immutable, write-through player-profile cache.
 *
 * <p>Updates for one player are chained, never raced, so concurrent event and
 * API awards cannot overwrite one another. Database work remains entirely
 * asynchronous in {@link ProfileRepository}.</p>
 */
public final class CachedProfileService implements ProfileService {

    private final ProfileRepository repository;
    private final ConcurrentHashMap<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<PlayerProfile>> updateChains = new ConcurrentHashMap<>();

    public CachedProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enable() {
        // Profiles are loaded lazily on player join or public API access.
    }

    @Override
    public void disable() {
        cache.values().forEach(profile -> repository.save(profile));
        cache.clear();
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> find(UUID playerId) {
        PlayerProfile cached = cache.get(playerId);
        return cached == null ? repository.find(playerId) : CompletableFuture.completedFuture(Optional.of(cached));
    }

    @Override
    public CompletableFuture<PlayerProfile> load(UUID playerId, String playerName) {
        PlayerProfile cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.find(playerId).thenApply(found -> {
            PlayerProfile profile = found.orElseGet(() -> new PlayerProfile(playerId, playerName, 1, 0, 0, ""));
            cache.put(playerId, profile);
            return profile;
        });
    }

    @Override
    public CompletableFuture<PlayerProfile> update(UUID playerId, String playerName, UnaryOperator<PlayerProfile> mutation) {
        synchronized (updateChains) {
            CompletableFuture<PlayerProfile> previous = updateChains.get(playerId);
            CompletableFuture<PlayerProfile> base = previous == null ? load(playerId, playerName) : previous;
            CompletableFuture<PlayerProfile> next = base.thenApply(mutation)
                    .thenCompose(profile -> repository.save(profile).thenApply(ignored -> profile))
                    .thenApply(profile -> {
                        cache.put(playerId, profile);
                        return profile;
                    });
            updateChains.put(playerId, next);
            next.whenComplete((ignored, throwable) -> updateChains.remove(playerId, next));
            return next;
        }
    }

    @Override
    public Optional<PlayerProfile> cached(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }

    @Override
    public CompletableFuture<Void> incrementStatistic(UUID playerId, String key, long amount) {
        return repository.incrementStatistic(playerId, key, amount);
    }
}
