package com.halilibrahim.halilibrahimmmo.energy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnergyStateTest {

    @Test
    void consumesOnlyAvailableEnergyAndRegeneratesUpToTheMaximum() {
        EnergyState energy = new EnergyState(100);

        assertTrue(energy.tryConsume(30));
        assertEquals(70, energy.current());
        assertFalse(energy.tryConsume(71));
        assertEquals(70, energy.current());
        assertTrue(energy.regenerate(100, 25));
        assertEquals(95, energy.current());
        assertTrue(energy.regenerate(100, 25));
        assertEquals(100, energy.current());
        assertFalse(energy.regenerate(100, 4));
    }
}
