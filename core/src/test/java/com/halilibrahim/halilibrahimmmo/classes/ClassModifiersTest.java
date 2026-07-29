package com.halilibrahim.halilibrahimmmo.classes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClassModifiersTest {
    @Test void appliesOnlyConfiguredAbilityModifiersAndKeepsZeroCostFree() {
        ClassModifiers modifiers = new ClassModifiers(2, .06, 5,
                Map.of("avci-odagi", .20), Map.of("avci-odagi", .20), Map.of("kan-yemini", 4),
                Map.of("avci-odagi", .40), Map.of("yikici-darbe", .32), Map.of());
        assertEquals(16, modifiers.adjustedEnergyCost("avci-odagi", 20));
        assertEquals(20, modifiers.adjustedEnergyCost("diger", 20));
        assertEquals(0, modifiers.adjustedEnergyCost("avci-odagi", 0));
        assertEquals(24_000, modifiers.adjustedCooldownMillis("avci-odagi", 30_000));
        assertEquals(14, modifiers.adjustedLimit("kan-yemini", 10));
    }

    @Test void extendsDurationAndStrengthensEffectOnlyForTheirOwnAbility() {
        ClassModifiers modifiers = new ClassModifiers(0, 0, 0,
                Map.of(), Map.of(), Map.of(),
                Map.of("avci-odagi", .40), Map.of("yikici-darbe", .32), Map.of());
        assertEquals(42_000, modifiers.adjustedDurationMillis("avci-odagi", 30_000));
        assertEquals(30_000, modifiers.adjustedDurationMillis("diger", 30_000));
        assertEquals(11.88, modifiers.adjustedEffectValue("yikici-darbe", 9.0), 1e-9);
        assertEquals(9.0, modifiers.adjustedEffectValue("diger", 9.0), 1e-9);
    }

    /**
     * The per-block drain is a flat subtraction, not a share, and it is floored
     * at one - a sweep that pays nothing per block would be bounded only by its
     * own duration, which is the state the drain exists to prevent.
     */
    @Test void lowersPerBlockEnergyButNeverToNothing() {
        ClassModifiers modifiers = new ClassModifiers(0, 0, 0,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of("bulldozer", 2));
        assertEquals(1, modifiers.adjustedEnergyPerBlock("bulldozer", 3));
        assertEquals(1, modifiers.adjustedEnergyPerBlock("bulldozer", 2));
        assertEquals(3, modifiers.adjustedEnergyPerBlock("diger", 3));
        assertEquals(0, modifiers.adjustedEnergyPerBlock("bulldozer", 0));
    }

    @Test void noneAppliesNothing() {
        ClassModifiers none = ClassModifiers.none();
        assertEquals(20, none.adjustedEnergyCost("avci-odagi", 20));
        assertEquals(30_000, none.adjustedDurationMillis("avci-odagi", 30_000));
        assertEquals(9.0, none.adjustedEffectValue("yikici-darbe", 9.0), 1e-9);
        assertEquals(5, none.adjustedEnergyPerBlock("agac-kirici", 5));
    }
}
