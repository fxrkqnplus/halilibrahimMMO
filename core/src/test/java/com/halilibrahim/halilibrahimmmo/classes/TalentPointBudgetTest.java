package com.halilibrahim.halilibrahimmmo.classes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TalentPointBudgetTest {
    @Test void carriesFourPointsAcrossEachPrestigeCycle() {
        assertEquals(0, TalentPointBudget.pointsFor(1, 0, 25));
        assertEquals(4, TalentPointBudget.pointsFor(100, 0, 25));
        assertEquals(4, TalentPointBudget.pointsFor(1, 1, 25));
        assertEquals(8, TalentPointBudget.pointsFor(100, 1, 25));
    }

    @Test void rejectsInvalidBudgetInputs() {
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(0, 0, 25));
        assertThrows(IllegalArgumentException.class, () -> TalentPointBudget.pointsFor(1, -1, 25));
    }
}
