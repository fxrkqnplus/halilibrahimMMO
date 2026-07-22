package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Owns the Güç a player has permanently banked across past prestiges - added
 * on top of the live sum of skill levels ({@code ProgressionService}) so
 * prestiging (which resets skill levels to 1) never lowers a player's
 * displayed Güç. See {@code YamlPrestigeService#prestige} for where this is
 * written.
 */
public interface BankedPowerService extends PluginService {

    /** Loads (and caches) a player's banked Güç, defaulting to 0 if never prestiged. */
    CompletableFuture<Integer> load(UUID playerId);

    /** Synchronous, in-memory only - {@code 0} until {@link #load(UUID)} has warmed the cache. */
    int cached(UUID playerId);

    /** Overwrites the player's banked Güç - always the pre-reset {@code cachedGlobalLevel}, never additive (it already includes any prior banked value). */
    CompletableFuture<Void> set(UUID playerId, int bankedPower);

    void evict(UUID playerId);
}
