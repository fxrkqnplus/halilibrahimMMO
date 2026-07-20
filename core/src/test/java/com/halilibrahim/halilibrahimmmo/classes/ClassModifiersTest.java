package com.halilibrahim.halilibrahimmmo.classes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClassModifiersTest {
    @Test void appliesOnlyConfiguredAbilityModifiersAndKeepsZeroCostFree() {
        ClassModifiers modifiers = new ClassModifiers(2, .06, 5, Map.of("avci-odagi", .20), Map.of("avci-odagi", .20), Map.of("damar-kirici", 4));
        assertEquals(16, modifiers.adjustedEnergyCost("avci-odagi", 20));
        assertEquals(20, modifiers.adjustedEnergyCost("diger", 20));
        assertEquals(0, modifiers.adjustedEnergyCost("avci-odagi", 0));
        assertEquals(24_000, modifiers.adjustedCooldownMillis("avci-odagi", 30_000));
        assertEquals(14, modifiers.adjustedLimit("damar-kirici", 10));
    }
}
