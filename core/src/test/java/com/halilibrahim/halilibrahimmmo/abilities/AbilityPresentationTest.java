package com.halilibrahim.halilibrahimmmo.abilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two presentation questions {@link AbilityEffectType} answers, pinned as
 * tests because both are curated lists rather than derivable rules - the kind of
 * thing a later phase can widen by accident.
 */
class AbilityPresentationTest {

    /**
     * Faz 24, madde 4. Exactly six abilities tint the screen edge; the other six
     * announce themselves in the world instead. If this set ever grows back
     * towards "all of them", the signal stops meaning anything.
     */
    @Test
    @DisplayName("Exactly the six named effect types tint the screen")
    void onlySixEffectTypesTintTheScreen() {
        Set<AbilityEffectType> expected = EnumSet.of(
                AbilityEffectType.PHANTOM_STEP,
                AbilityEffectType.TOOL_EFFICIENCY,
                AbilityEffectType.NOBLE_WOLF,
                AbilityEffectType.GROWTH_SURGE,
                AbilityEffectType.LOVE_CALL,
                AbilityEffectType.LIFE_STEAL
        );
        Set<AbilityEffectType> actual = EnumSet.noneOf(AbilityEffectType.class);
        for (AbilityEffectType type : AbilityEffectType.values()) {
            if (type.tintsScreen()) {
                actual.add(type);
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Both frenzy abilities also tint, since the pulse has nothing to pulse otherwise")
    void everyFrenzyAlsoTints() {
        for (AbilityEffectType type : AbilityEffectType.values()) {
            if (type.isFrenzy()) {
                assertTrue(type.tintsScreen(), type + " çılgınlık ama ekranı boyamıyor.");
            }
        }
    }

    @Test
    @DisplayName("The two frenzies are the combat pair, and nothing else has joined them")
    void frenzyIsStillJustTheTwo() {
        assertTrue(AbilityEffectType.LIFE_STEAL.isFrenzy());
        assertTrue(AbilityEffectType.NOBLE_WOLF.isFrenzy());
        assertFalse(AbilityEffectType.DAMAGE_BUFF.isFrenzy());
        assertFalse(AbilityEffectType.GUARDIAN_CALL.isFrenzy());
    }
}
