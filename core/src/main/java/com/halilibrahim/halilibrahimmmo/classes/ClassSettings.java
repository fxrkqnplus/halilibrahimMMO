package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/** Immutable parsed {@code classes.yml} snapshot. */
public record ClassSettings(
        long changeCooldownMillis,
        int levelsPerTalentPoint,
        Map<String, PlayerClassDefinition> definitions
) {
}
