package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrestigeRulesTest {

    private static final Map<String, Integer> ALL_25 = Map.of("kazicilik", 25, "odunculuk", 25, "madencilik", 25, "avcilik", 25);

    @Test
    void requiresEverySkillAtMinimumAndClassAndSynergySkillAtCap() {
        PrestigeEligibilityInput input = new PrestigeEligibilityInput(ALL_25, 25, true, 50, 50, 100, 100, 2, 10);
        PrestigeEligibility eligibility = PrestigeRules.eligibility(input);
        assertEquals(PrestigeResult.Status.PRESTIGED, eligibility.status());
        assertTrue(eligibility.report().allMet());
    }

    @Test
    void onlyIncrementsPrestigeAndPreservesSelectedClass() {
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Oyuncu", 1, 0, 2, "madenci");
        PlayerProfile updated = PrestigeRules.apply(profile);
        assertEquals(3, updated.prestige());
        assertEquals("madenci", updated.selectedClass());
    }

    @Test
    void rejectsWhenAnySkillIsBelowMinimum() {
        Map<String, Integer> oneShort = Map.of("kazicilik", 24, "odunculuk", 25, "madencilik", 25, "avcilik", 25);
        PrestigeEligibilityInput input = new PrestigeEligibilityInput(oneShort, 25, true, 50, 50, 100, 100, 0, 10);
        PrestigeEligibility eligibility = PrestigeRules.eligibility(input);
        assertEquals(PrestigeResult.Status.REQUIREMENTS_NOT_MET, eligibility.status());
        assertFalse(eligibility.report().skillsMet().get("kazicilik"));
    }

    @Test
    void rejectsWhenNoClassIsSelected() {
        PrestigeEligibilityInput input = new PrestigeEligibilityInput(ALL_25, 25, false, 0, 50, 0, 100, 0, 10);
        PrestigeEligibility eligibility = PrestigeRules.eligibility(input);
        assertEquals(PrestigeResult.Status.REQUIREMENTS_NOT_MET, eligibility.status());
        assertFalse(eligibility.report().classSelected());
    }

    @Test
    void rejectsWhenClassLevelOrSynergySkillIsBelowCap() {
        PrestigeEligibilityInput belowClassCap = new PrestigeEligibilityInput(ALL_25, 25, true, 49, 50, 100, 100, 0, 10);
        assertEquals(PrestigeResult.Status.REQUIREMENTS_NOT_MET, PrestigeRules.eligibility(belowClassCap).status());

        PrestigeEligibilityInput belowSynergyCap = new PrestigeEligibilityInput(ALL_25, 25, true, 50, 50, 99, 100, 0, 10);
        assertEquals(PrestigeResult.Status.REQUIREMENTS_NOT_MET, PrestigeRules.eligibility(belowSynergyCap).status());
    }

    @Test
    void reportsMaximumReachedEvenWhenOtherwiseEligible() {
        PrestigeEligibilityInput input = new PrestigeEligibilityInput(ALL_25, 25, true, 50, 50, 100, 100, 10, 10);
        assertEquals(PrestigeResult.Status.MAXIMUM_REACHED, PrestigeRules.eligibility(input).status());
    }
}
