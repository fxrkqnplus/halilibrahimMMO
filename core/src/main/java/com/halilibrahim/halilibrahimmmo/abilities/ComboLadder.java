package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;

/**
 * How savage the frenzy line at the bottom of the screen has got.
 *
 * <p>Driven by the <strong>total damage dealt</strong> since the ability
 * started, not by a kill count. A kill counter reads a chicken and a ravager as
 * the same achievement, and the whole point of the ladder is that it should be
 * hard to climb - so it counts what the player actually put out.</p>
 *
 * <p>The rungs are code and the words are {@code messages.yml}, the same split
 * every bounded value in this project uses: an owner can rewrite what each rung
 * shouts without being able to invent a rung that nothing ever reaches.</p>
 *
 * <p>Pure and dependency-free so the thresholds can be argued about in a test
 * rather than in a live fight.</p>
 */
public final class ComboLadder {

    /** Cumulative damage needed to reach each rung, lowest first. */
    private static final double[] THRESHOLDS = {1.0D, 40.0D, 100.0D, 200.0D, 350.0D, 550.0D};

    private static final MessageKey[] LINES = {
            MessageKey.ABILITY_COMBO_1, MessageKey.ABILITY_COMBO_2, MessageKey.ABILITY_COMBO_3,
            MessageKey.ABILITY_COMBO_4, MessageKey.ABILITY_COMBO_5, MessageKey.ABILITY_COMBO_6
    };

    /**
     * From this rung upward the client shakes the line rather than only
     * colouring it. Kept here rather than on the client so "which rungs are
     * loud" is one number in one place.
     */
    public static final int SHAKING_TIER = 5;

    private ComboLadder() {
        throw new UnsupportedOperationException("ComboLadder is a static utility class");
    }

    /** @return {@code 0} before the first hit lands, otherwise 1 to {@code 6} */
    public static int tierFor(double totalDamage) {
        int tier = 0;
        for (int index = 0; index < THRESHOLDS.length; index++) {
            if (totalDamage >= THRESHOLDS[index]) {
                tier = index + 1;
            }
        }
        return tier;
    }

    /** The line this rung shouts, or {@code null} at rung zero, where there is nothing to say yet. */
    public static MessageKey lineFor(int tier) {
        return tier < 1 || tier > LINES.length ? null : LINES[tier - 1];
    }
}
