package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Computes Güç (global power), which is the sum of every configured skill's
 * level plus any Güç a player has permanently banked across past prestiges
 * (see {@code prestige.BankedPowerService}) - Güç itself still has no XP or
 * level track of its own, and no code anywhere awards it directly.
 */
public interface ProgressionService extends PluginService {

    /** Synchronous, in-memory only - sums each currently cached skill level, defaulting an unloaded skill to level 1. */
    int cachedGlobalLevel(UUID playerId);

    /** Asynchronous equivalent of {@link #cachedGlobalLevel(UUID)}, loading any not-yet-cached skill from the database. */
    CompletableFuture<Integer> globalLevel(UUID playerId);
}
