package com.halilibrahim.halilibrahimmmo.api.skills;

/** Immutable, read-only definition of a configured HalilIbrahimMMO skill. */
public record SkillDefinitionSnapshot(String id, String displayName, int maximumLevel) {
}
