package com.halilibrahim.halilibrahimmmo.api.profile;

import java.util.UUID;

/**
 * Immutable public view of a player's persistent MMO progression.
 *
 * <p>An empty {@link #selectedClass()} means that the player has not selected a
 * class yet. This keeps the public API dependency-free while avoiding nullable
 * profile fields.</p>
 */
public record PlayerProfileSnapshot(
        UUID playerId,
        String playerName,
        int globalLevel,
        long globalExperience,
        int prestige,
        String selectedClass
) {
}
