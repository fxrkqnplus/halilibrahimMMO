package com.halilibrahim.halilibrahimmmo.skills;

import java.util.Map;

/** Complete immutable skills.yml snapshot. */
public record SkillSettings(SkillExperienceCurve curve, Map<String, SkillDefinition> definitions) {
}
