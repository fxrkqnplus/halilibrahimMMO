package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class PrestigeRulesTest {
    @Test void requiresMaximumLevelAndResetsOnlyGlobalProgress() {
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Oyuncu", 100, 0, 2, "madenci");
        assertEquals(PrestigeResult.Status.PRESTIGED, PrestigeRules.eligibility(profile, 100, 10));
        PlayerProfile updated = PrestigeRules.apply(profile);
        assertEquals(1, updated.globalLevel()); assertEquals(0, updated.globalExperience()); assertEquals(3, updated.prestige()); assertEquals("madenci", updated.selectedClass());
    }
    @Test void rejectsIncompleteOrCappedPrestige() {
        PlayerProfile incomplete = new PlayerProfile(UUID.randomUUID(), "Oyuncu", 99, 10, 0, "");
        PlayerProfile capped = new PlayerProfile(UUID.randomUUID(), "Oyuncu", 100, 0, 10, "");
        assertEquals(PrestigeResult.Status.LEVEL_REQUIRED, PrestigeRules.eligibility(incomplete, 100, 10));
        assertEquals(PrestigeResult.Status.MAXIMUM_REACHED, PrestigeRules.eligibility(capped, 100, 10));
    }
}
