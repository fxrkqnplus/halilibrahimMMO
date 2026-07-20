package com.halilibrahim.halilibrahimmmo.classes;

/** Pure shared-talent budget calculation used across class and prestige flows. */
public final class TalentPointBudget {
    private TalentPointBudget() { throw new UnsupportedOperationException("TalentPointBudget is a static utility class"); }
    public static int pointsFor(int globalLevel, int prestige, int levelsPerPoint) {
        if (globalLevel < 1 || prestige < 0 || levelsPerPoint < 1) throw new IllegalArgumentException("Invalid talent point inputs.");
        return Math.addExact(Math.multiplyExact(prestige, 4), globalLevel / levelsPerPoint);
    }
}
