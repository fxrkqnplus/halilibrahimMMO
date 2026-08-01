package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Saygınlık Puanı: one lifetime-cumulative number per player, counting what
 * they have actually taken out of the world rather than how far they have
 * levelled.
 *
 * <p>It is deliberately <strong>not</strong> a currency. Nothing is ever spent,
 * nothing is ever traded, and no amount of it buys anything - this project has
 * had no economy since Phase 1 and still has none. It is a score: it decides
 * leaderboard standing, unlocks titles and cosmetics, and marks the moments
 * worth telling the whole server about. Every gaining source is one-shot by
 * construction (a world-generated block can only be mined once, a quest
 * completes once, a boss dies once), so there is nothing here to farm.</p>
 *
 * <p><strong>Faz 23 changed what this number means.</strong> It used to be
 * "what you have taken out of the world" and could only ever rise. It is now
 * standing, and standing can be lost: dying costs points on a scale set by how
 * much you had (see {@link ReputationPenalty}). Nothing else about it changed -
 * it is still not spendable, still not transferable, and still cannot go below
 * zero.</p>
 */
public interface ReputationService extends PluginService {

    ReputationSettings settings();

    /** Synchronous, in-memory only - {@code 0} until {@link #load} has warmed this player on join. */
    long cached(UUID playerId);

    CompletableFuture<Long> load(UUID playerId);

    /**
     * Adds {@code amount} points, serialised per player so two awards landing in
     * the same tick cannot overwrite each other.
     *
     * @return empty when {@code amount} is not positive - the overwhelming
     *         majority of block breaks are worth nothing and must not cost a
     *         database round trip.
     */
    CompletableFuture<Optional<ReputationAward>> award(UUID playerId, long amount);

    /** Convenience for the block-break path: resolves the configured value, then awards it. */
    CompletableFuture<Optional<ReputationAward>> awardBlock(UUID playerId, Material material);

    /** Convenience for the kill path: resolves the configured value, then awards it. */
    CompletableFuture<Optional<ReputationAward>> awardEntity(UUID playerId, EntityType entityType);

    /** Convenience for quest completion, which is how discovering somewhere valuable pays out. */
    CompletableFuture<Optional<ReputationAward>> awardQuest(UUID playerId, String questId);

    /**
     * Takes {@code amount} points off, clamped so a total can reach zero but
     * never pass it.
     *
     * <p>Rides the same per-player serial chain awards do, so a death landing in
     * the same tick as a boss reward cannot lose either of them. The caller
     * decides <em>how much</em>; this only applies it.</p>
     *
     * @return the number of points actually lost, which is zero when there was
     *         nothing to take
     */
    CompletableFuture<Long> deduct(UUID playerId, long amount);

    /**
     * Internal, admin-only: overwrites a player's lifetime total outright.
     *
     * <p>The only path in the plugin that may lower this number. Gameplay never
     * subtracts - the score means "what you took out of the world", and taking
     * it back would make it mean something else - but staff still need a way to
     * correct a total that a bug or a rollback left wrong. Works for offline
     * players, and rides the same per-player serial chain awards use, so it can
     * never interleave with one.</p>
     */
    CompletableFuture<Long> adminSetTotal(UUID playerId, long total);

    void evict(UUID playerId);
}
