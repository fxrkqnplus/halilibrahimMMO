package com.halilibrahim.halilibrahimmmo.activation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Write-through, join-warmed cache over {@link ActivationRepository}. */
public final class CachedActivationService implements ActivationService {

    private final ActivationRepository repository;
    private final ConcurrentHashMap<UUID, Boolean> cache = new ConcurrentHashMap<>();

    public CachedActivationService(ActivationRepository repository) {
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
    public CompletableFuture<Boolean> isActivated(UUID playerId) {
        Boolean cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.isActivated(playerId).thenApply(activated -> {
            cache.put(playerId, activated);
            return activated;
        });
    }

    @Override
    public boolean cached(UUID playerId) {
        return cache.getOrDefault(playerId, false);
    }

    @Override
    public CompletableFuture<Void> activate(UUID playerId) {
        if (cache.getOrDefault(playerId, false)) {
            return CompletableFuture.completedFuture(null);
        }
        return repository.activate(playerId).thenAccept(ignored -> cache.put(playerId, true));
    }

    @Override
    public CompletableFuture<List<ActivatedPlayer>> findAllActivated() {
        return repository.findAllActivated();
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }
}
