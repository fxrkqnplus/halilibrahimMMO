package com.halilibrahim.halilibrahimmmo.skills;

import java.util.Map;

/** Validated, explicit experience curve shared by every configured skill. */
public final class SkillExperienceCurve {

    private final int maximumLevel;
    private final Map<Integer, Long> requirements;

    public SkillExperienceCurve(int maximumLevel, Map<Integer, Long> requirements) {
        if (maximumLevel < 2) {
            throw new IllegalArgumentException("Maximum skill level must be at least 2.");
        }
        for (int level = 1; level <= maximumLevel; level++) {
            Long required = requirements.get(level);
            if (required == null || required < 0 || (level < maximumLevel && required == 0)) {
                throw new IllegalArgumentException("Every skill level must have a valid explicit XP requirement.");
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

    public SkillProgress award(SkillProgress progress, long amount) {
        if (amount <= 0 || progress.level() >= maximumLevel) {
            return progress;
        }
        int level = progress.level();
        long experience = Math.addExact(progress.experience(), amount);
        while (level < maximumLevel && experience >= requirements.get(level)) {
            experience -= requirements.get(level);
            level++;
        }
        return new SkillProgress(progress.playerId(), progress.skillId(), level,
                level == maximumLevel ? 0 : experience);
    }
}
