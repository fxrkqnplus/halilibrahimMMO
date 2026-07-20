package com.halilibrahim.halilibrahimmmo.api;

import java.util.UUID;

/**
 * Synchronous, in-memory-only view of a player's most recently loaded
 * progression state - for callers that cannot await a
 * {@link java.util.concurrent.CompletableFuture} (PlaceholderAPI, LuckPerms
 * context calculators). {@code selectedClassId} is the raw configured class
 * id (empty if none selected); {@code equippedTitle}/{@code equippedCosmetic}
 * are already-resolved display names (empty if none equipped), since those
 * two only ever feed player-facing text, never programmatic matching.
 */
public record CachedPlayerSnapshot(
        UUID playerId,
        int globalLevel,
        long globalExperience,
        long experienceToNextLevel,
        int prestige,
        String selectedClassId,
        String equippedTitle,
        String equippedCosmetic
) {
}
