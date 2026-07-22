package com.halilibrahim.halilibrahimmmo.onboarding;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Write-through, join-warmed cache over {@link OnboardingRepository}, mirroring {@code activation.CachedActivationService}. */
public final class CachedOnboardingService implements OnboardingService {

    private final OnboardingRepository repository;
    private final ConcurrentHashMap<UUID, Set<String>> cache = new ConcurrentHashMap<>();

    public CachedOnboardingService(OnboardingRepository repository) {
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
    public CompletableFuture<Set<String>> loadCompleted(UUID playerId) {
        Set<String> cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.findCompleted(playerId).thenApply(found -> {
            cache.put(playerId, found);
            return found;
        });
    }

    @Override
    public Set<String> cachedCompleted(UUID playerId) {
        return cache.getOrDefault(playerId, Set.of());
    }

    @Override
    public CompletableFuture<Boolean> complete(UUID playerId, String playerName, OnboardingTask task) {
        return loadCompleted(playerId).thenCompose(completed -> {
            if (completed.contains(task.id())) {
                return CompletableFuture.completedFuture(false);
            }
            return repository.markCompleted(playerId, task.id()).thenApply(ignored -> {
                Set<String> updated = new LinkedHashSet<>(completed);
                updated.add(task.id());
                cache.put(playerId, Set.copyOf(updated));
                return true;
            });
        });
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }
}
