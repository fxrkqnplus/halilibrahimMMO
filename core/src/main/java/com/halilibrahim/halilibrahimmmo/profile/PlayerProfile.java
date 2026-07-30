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
    /**
     * What one prestige rank adds to every XP this player earns, as a fraction.
     *
     * <p>Raised from a quarter to a half in Faz 22. Prestige resets six skill
     * levels and a class level, which is hours of play; a quarter more XP per
     * rank paid that back too slowly to read as a reward, and the whole point
     * of the reset is that the next climb is faster than the last one.</p>
     */
    public static final double EXPERIENCE_BONUS_PER_PRESTIGE = 0.50D;

    /**
     * The multiplier this player's prestige rank applies to earned XP, both in
     * skills and in their class track.
     *
     * <p>Lives on the profile because both consumers - the skill-side synergy
     * lambda in the plugin bootstrap and {@code YamlClassService} - already hold
     * one, and each of them used to spell the arithmetic out for itself. Two
     * copies of a growth rate is exactly the kind of thing that ends up
     * disagreeing after a balance pass.</p>
     */
    public double prestigeExperienceMultiplier() {
        return 1.0D + EXPERIENCE_BONUS_PER_PRESTIGE * prestige;
    }

    public PlayerProfileSnapshot snapshot() {
        return new PlayerProfileSnapshot(playerId, playerName, globalLevel, globalExperience, prestige, selectedClass);
    }
}
