package com.halilibrahim.halilibrahimmmo.onboarding;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Tracks which one-time onboarding tutorial nudges a player has already completed. */
public interface OnboardingService extends PluginService {

    /** Loads (and caches) every task id this player has ever completed. */
    CompletableFuture<Set<String>> loadCompleted(UUID playerId);

    /** Synchronous, in-memory only - empty until {@link #loadCompleted} has warmed the cache. */
    Set<String> cachedCompleted(UUID playerId);

    /** Idempotent: marks {@code task} completed. @return {@code true} only the first time (never on repeat calls). */
    CompletableFuture<Boolean> complete(UUID playerId, String playerName, OnboardingTask task);

    void evict(UUID playerId);
}
