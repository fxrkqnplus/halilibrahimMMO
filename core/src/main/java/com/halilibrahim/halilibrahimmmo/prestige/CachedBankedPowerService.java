package com.halilibrahim.halilibrahimmmo.prestige;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Write-through, join-warmed cache over {@link BankedPowerRepository}. */
public final class CachedBankedPowerService implements BankedPowerService {

    private final BankedPowerRepository repository;
    private final ConcurrentHashMap<UUID, Integer> cache = new ConcurrentHashMap<>();

    public CachedBankedPowerService(BankedPowerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enable() {
        // No YAML settings - reload() stays the PluginService default no-op.
    }

    @Override
    public void disable() {
        cache.clear();
    }

    @Override
    public CompletableFuture<Integer> load(UUID playerId) {
        Integer cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.find(playerId).thenApply(bankedPower -> {
            cache.put(playerId, bankedPower);
            return bankedPower;
        });
    }

    @Override
    public int cached(UUID playerId) {
        return cache.getOrDefault(playerId, 0);
    }

    @Override
    public CompletableFuture<Void> set(UUID playerId, int bankedPower) {
        return repository.save(playerId, bankedPower).thenAccept(ignored -> cache.put(playerId, bankedPower));
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }
}
