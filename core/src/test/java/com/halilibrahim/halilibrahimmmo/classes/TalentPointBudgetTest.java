package com.halilibrahim.halilibrahimmmo.classes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TalentPointBudgetTest {

    /** The shipped {@code classes.yml} cadences: 25 Güç/4-per-prestige features, 40 Güç/2-per-prestige abilities. */
    private static final TalentPointSettings SHIPPED = new TalentPointSettings(25, 40, 4, 2);

    @Test void carriesPrestigeGrantsAcrossEachCycle() {
        assertEquals(0, TalentPointBudget.pointsFor(1, 0, 25, 4));
        assertEquals(4, TalentPointBudget.pointsFor(100, 0, 25, 4));
        assertEquals(4, TalentPointBudget.pointsFor(1, 1, 25, 4));
        assertEquals(8, TalentPointBudget.pointsFor(100, 1, 25, 4));
    }

    @Test void budgetsTheTwoPoolsIndependently() {
        assertEquals(4, TalentPointBudget.pointsFor(100, 0, SHIPPED, TalentPointCategory.OZELLIK));
        assertEquals(2, TalentPointBudget.pointsFor(100, 0, SHIPPED, TalentPointCategory.KABILIYET));
        assertEquals(8, TalentPointBudget.pointsFor(100, 1, SHIPPED, TalentPointCategory.OZELLIK));
        assertEquals(4, TalentPointBudget.pointsFor(100, 1, SHIPPED, TalentPointCategory.KABILIYET));
    }

    @Test void rejectsInvalidBudgetInputs() {
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(0, 0, 25, 4));
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(1, -1, 25, 4));
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(1, 0, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(1, 0, 25, -1));
    }
}
