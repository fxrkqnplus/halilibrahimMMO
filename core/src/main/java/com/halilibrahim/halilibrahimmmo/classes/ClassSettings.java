package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/** Immutable parsed {@code classes.yml} snapshot. */
public record ClassSettings(
        long changeCooldownMillis,
        TalentPointSettings talentPoints,
        ClassExperienceCurve classLevelCurve,
        double synergyExperienceBonus,
        Map<String, PlayerClassDefinition> definitions
) {
}
