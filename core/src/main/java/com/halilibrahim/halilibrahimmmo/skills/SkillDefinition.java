package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.Material;

import java.util.Set;

/** Immutable YAML-authored skill definition. */
public record SkillDefinition(String id, String displayName, SkillSource source, Set<Material> materials, long reward) {
}
