package com.halilibrahim.halilibrahimmmo.prestige;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faz 24, madde 17: the prestige screen offers two rewards, and the two that
 * left the screen did not leave the enum.
 */
class PrestigeRewardSelectionTest {

    @Test
    @DisplayName("The screen offers exactly two rewards")
    void twoRewardsAreSelectable() {
        assertEquals(2, PrestigeRewardType.SELECTABLE_COUNT);
        assertEquals(2, PrestigeRewardType.selectable().size());
    }

    @Test
    @DisplayName("They are the first two, in declaration order")
    void selectableAreTheFirstTwo() {
        assertEquals(List.of(PrestigeRewardType.CAN, PrestigeRewardType.HASAR), PrestigeRewardType.selectable());
    }

    /**
     * The load-bearing half of madde 17. Deleting the two constants would have
     * silently voided every ENERJI and BILGELIK a player had already banked,
     * because {@link PrestigeRewardType#byName} answers an unknown name with
     * nothing and the stored rows would stop resolving.
     */
    @Test
    @DisplayName("Rewards that left the screen still resolve, so banked ranks keep paying out")
    void retiredRewardsStillResolve() {
        assertTrue(PrestigeRewardType.byName("ENERJI").isPresent());
        assertTrue(PrestigeRewardType.byName("BILGELIK").isPresent());
        assertEquals(4, PrestigeRewardType.values().length);
    }
}
