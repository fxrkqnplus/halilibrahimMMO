package com.halilibrahim.halilibrahimmmo.classes;

/**
 * Pure per-category talent budget calculation used across class and prestige
 * flows. Both pools share one formula but carry their own cadence, so feature
 * and ability points can be tuned entirely independently in
 * {@code classes.yml} - ability points are deliberately the scarcer pool.
 */
public final class TalentPointBudget {
    private TalentPointBudget() { throw new UnsupportedOperationException("TalentPointBudget is a static utility class"); }

    public static int pointsFor(int globalLevel, int prestige, int levelsPerPoint, int pointsPerPrestige) {
        if (globalLevel < 1 || prestige < 0 || levelsPerPoint < 1 || pointsPerPrestige < 0) {
            throw new IllegalArgumentException("Invalid talent point inputs.");
        }
        return Math.addExact(Math.multiplyExact(prestige, pointsPerPrestige), globalLevel / levelsPerPoint);
    }

    public static int pointsFor(int globalLevel, int prestige, TalentPointSettings settings, TalentPointCategory category) {
        return category == TalentPointCategory.OZELLIK
                ? pointsFor(globalLevel, prestige, settings.levelsPerFeaturePoint(), settings.featurePointsPerPrestige())
                : pointsFor(globalLevel, prestige, settings.levelsPerAbilityPoint(), settings.abilityPointsPerPrestige());
    }
}
