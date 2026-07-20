package com.halilibrahim.halilibrahimmmo.profile;

import com.halilibrahim.halilibrahimmmo.api.profile.PlayerProfileSnapshot;

import java.util.UUID;

/** Immutable internal profile state used by cache and repository boundaries. */
public record PlayerProfile(
        UUID playerId,
        String playerName,
        int globalLevel,
        long globalExperience,
        int prestige,
        String selectedClass
) {
    public PlayerProfileSnapshot snapshot() {
        return new PlayerProfileSnapshot(playerId, playerName, globalLevel, globalExperience, prestige, selectedClass);
    }
}
