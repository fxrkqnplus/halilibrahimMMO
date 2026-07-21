package com.halilibrahim.halilibrahimmmo.activation;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Owns the one-time {@code /mmo aktif} onboarding gate: a brand-new player's
 * progression (global/skill/class XP, quests, collections) stays completely
 * inert until this is set, so the sidebar and every XP-granting listener can
 * check {@link #cached(UUID)} before doing anything.
 */
public interface ActivationService extends PluginService {

    /** Loads (and caches) whether a player has ever activated. */
    CompletableFuture<Boolean> isActivated(UUID playerId);

    /** Synchronous, in-memory only - {@code false} until {@link #isActivated} has warmed the cache. */
    boolean cached(UUID playerId);

    /** Idempotent: marks a player as activated, a no-op if already activated. */
    CompletableFuture<Void> activate(UUID playerId);

    void evict(UUID playerId);
}
