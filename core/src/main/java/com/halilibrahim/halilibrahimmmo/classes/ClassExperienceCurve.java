package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/** Validated, explicit experience curve shared by every class's own level track. */
public final class ClassExperienceCurve {

    private final int maximumLevel;
    private final Map<Integer, Long> requirements;

    public ClassExperienceCurve(int maximumLevel, Map<Integer, Long> requirements) {
        if (maximumLevel < 2) {
            throw new IllegalArgumentException("Maximum class level must be at least 2.");
        }
        for (int level = 1; level <= maximumLevel; level++) {
            Long required = requirements.get(level);
            if (required == null || required < 0 || (level < maximumLevel && required == 0)) {
                throw new IllegalArgumentException("Every class level must have a valid explicit XP requirement.");
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

    public ClassLevelProgress award(ClassLevelProgress progress, long amount) {
        if (amount <= 0 || progress.level() >= maximumLevel) {
            return progress;
        }
        int level = progress.level();
        long experience = Math.addExact(progress.experience(), amount);
        while (level < maximumLevel && experience >= requirements.get(level)) {
            experience -= requirements.get(level);
            level++;
        }
        return new ClassLevelProgress(progress.classId(), level, level == maximumLevel ? 0 : experience);
    }
}
