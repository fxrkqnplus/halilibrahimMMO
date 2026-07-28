package com.halilibrahim.halilibrahimmmo.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FeatureUnlockRulesTest {

    @Test
    void reachingTheRequiredClassLevelUnlocks() {
        assertTrue(FeatureUnlockRules.isUnlocked(15, 0, FeatureUnlockRules.HOME_CLASS_LEVEL));
        assertTrue(FeatureUnlockRules.isUnlocked(40, 0, FeatureUnlockRules.HOME_CLASS_LEVEL));
    }

    @Test
    void stayingBelowTheRequiredClassLevelDoesNot() {
        assertFalse(FeatureUnlockRules.isUnlocked(14, 0, FeatureUnlockRules.HOME_CLASS_LEVEL));
        assertFalse(FeatureUnlockRules.isUnlocked(0, 0, FeatureUnlockRules.HOME_CLASS_LEVEL));
    }

    /**
     * The load-bearing case: prestige resets the selected class to level 1, so
     * a purely level-based gate would take these features away from the players
     * who worked hardest for them, every single time they prestiged.
     */
    @Test
    void prestigeAloneUnlocksEvenAtClassLevelOne() {
        assertTrue(FeatureUnlockRules.isUnlocked(1, 1, FeatureUnlockRules.HOME_CLASS_LEVEL));
        assertTrue(FeatureUnlockRules.isUnlocked(1, 1, FeatureUnlockRules.TRADE_CLASS_LEVEL));
    }

    @Test
    void tradeAsksMoreOfTheInitiatorThanOfTheOtherSide() {
        assertTrue(FeatureUnlockRules.TRADE_CLASS_LEVEL > FeatureUnlockRules.TRADE_PARTNER_CLASS_LEVEL);
        assertFalse(FeatureUnlockRules.isUnlocked(15, 0, FeatureUnlockRules.TRADE_CLASS_LEVEL));
        assertTrue(FeatureUnlockRules.isUnlocked(15, 0, FeatureUnlockRules.TRADE_PARTNER_CLASS_LEVEL));
    }
}
