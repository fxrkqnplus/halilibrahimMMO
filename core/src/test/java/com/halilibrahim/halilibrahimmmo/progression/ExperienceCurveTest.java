package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ExperienceCurveTest {

    @Test
    void carriesExperienceAcrossSeveralLevelsAndCapsIt() {
        ExperienceCurve curve = new ExperienceCurve(3, Map.of(1, 100L, 2, 200L, 3, 0L));
        PlayerProfile profile = new PlayerProfile(UUID.randomUUID(), "Oyuncu", 1, 0, 0, "");

        PlayerProfile afterAward = curve.award(profile, 350L);

        assertEquals(3, afterAward.globalLevel());
        assertEquals(0L, afterAward.globalExperience());
    }

    @Test
    void rejectsNonContiguousOrInvalidDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> new ExperienceCurve(3, Map.of(1, 100L, 3, 0L)));
        assertThrows(IllegalArgumentException.class, () -> new ExperienceCurve(3, Map.of(1, 0L, 2, 100L, 3, 0L)));
    }
}
