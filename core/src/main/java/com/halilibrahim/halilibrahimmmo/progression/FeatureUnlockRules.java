package com.halilibrahim.halilibrahimmmo.progression;

/**
 * The one rule every class-level-gated convenience feature is unlocked by.
 *
 * <p>Prestige always counts as satisfying the gate, whatever the required
 * level is, and that is not a courtesy: prestige resets the selected class back
 * to level 1, so a purely level-based gate would take a feature away from the
 * exact players who worked hardest for it, permanently, every single time they
 * prestiged.</p>
 *
 * <p>Pure and dependency-free so it can be unit-tested in isolation, the same
 * shape as {@code PrestigeRules} and {@code QuestResetRules}.</p>
 */
public final class FeatureUnlockRules {

    /** Class level at which {@code /ev} unlocks. */
    public static final int HOME_CLASS_LEVEL = 15;
    /** Class level at which a player may start a trade. */
    public static final int TRADE_CLASS_LEVEL = 20;
    /** Class level a trade's other side must have reached to be traded with. */
    public static final int TRADE_PARTNER_CLASS_LEVEL = 15;
    /**
     * Class level at which {@code /çanta} unlocks - the highest of the three,
     * because it is the only one that gives storage nothing can take away.
     */
    public static final int BACKPACK_CLASS_LEVEL = 30;

    private FeatureUnlockRules() {
        throw new UnsupportedOperationException("FeatureUnlockRules is a static utility class");
    }

    public static boolean isUnlocked(int classLevel, int prestige, int requiredClassLevel) {
        return prestige > 0 || classLevel >= requiredClassLevel;
    }
}
