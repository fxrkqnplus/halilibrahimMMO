package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;

import java.util.Map;

/** Pure, validated global XP curve with a hard configurable level cap. */
public final class ExperienceCurve {

    private final int maximumLevel;
    private final Map<Integer, Long> requirements;

    public ExperienceCurve(int maximumLevel, Map<Integer, Long> requirements) {
        if (maximumLevel < 2) {
            throw new IllegalArgumentException("Maximum level must be at least 2.");
        }
        for (int level = 1; level <= maximumLevel; level++) {
            Long required = requirements.get(level);
            if (required == null || required < 0 || (level < maximumLevel && required == 0)) {
                throw new IllegalArgumentException("Every level must have a valid explicit XP requirement.");
            }
        }
        this.maximumLevel = maximumLevel;
        this.requirements = Map.copyOf(requirements);
    }

    public int maximumLevel() {
        return maximumLevel;
    }

    public long requiredForCurrentLevel(int level) {
        return requirements.get(level);
    }

    public PlayerProfile award(PlayerProfile profile, long amount) {
        if (amount <= 0 || profile.globalLevel() >= maximumLevel) {
            return profile;
        }
        int level = profile.globalLevel();
        long experience = Math.addExact(profile.globalExperience(), amount);
        while (level < maximumLevel && experience >= requirements.get(level)) {
            experience -= requirements.get(level);
            level++;
        }
        if (level == maximumLevel) {
            experience = 0;
        }
        return new PlayerProfile(profile.playerId(), profile.playerName(), level, experience,
                profile.prestige(), profile.selectedClass());
    }
}
