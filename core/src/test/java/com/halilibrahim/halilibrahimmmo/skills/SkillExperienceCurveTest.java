package com.halilibrahim.halilibrahimmmo.skills;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SkillExperienceCurveTest {

    @Test
    void carriesExperienceAcrossMultipleLevelsAndDiscardsItAtTheCap() {
        SkillExperienceCurve curve = new SkillExperienceCurve(3, Map.of(1, 100L, 2, 150L, 3, 0L));
        SkillProgress start = new SkillProgress(UUID.randomUUID(), "madencilik", 1, 0);

        SkillProgress progressed = curve.award(start, 260);
        assertEquals(3, progressed.level());
        assertEquals(0, progressed.experience());
    }

    @Test
    void rejectsIncompleteOrInvalidExplicitTables() {
        assertThrows(IllegalArgumentException.class, () ->
                new SkillExperienceCurve(3, Map.of(1, 100L, 2, 0L, 3, 0L)));
        assertThrows(IllegalArgumentException.class, () ->
                new SkillExperienceCurve(3, Map.of(1, 100L, 3, 0L)));
    }
}
