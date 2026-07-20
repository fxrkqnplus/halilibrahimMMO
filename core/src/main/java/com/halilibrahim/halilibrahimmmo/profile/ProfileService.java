package com.halilibrahim.halilibrahimmmo.profile;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/** Persistent, asynchronous player-profile access contract. */
public interface ProfileService extends PluginService {

    CompletableFuture<Optional<PlayerProfile>> find(UUID playerId);

    CompletableFuture<PlayerProfile> load(UUID playerId, String playerName);

    CompletableFuture<PlayerProfile> update(UUID playerId, String playerName, UnaryOperator<PlayerProfile> mutation);

    CompletableFuture<Void> incrementStatistic(UUID playerId, String key, long amount);

    /** Synchronous, in-memory only - for callers that cannot await a future (e.g. PlaceholderAPI). */
    Optional<PlayerProfile> cached(UUID playerId);

    void evict(UUID playerId);
}
