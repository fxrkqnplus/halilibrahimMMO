package com.halilibrahim.halilibrahimmmo.collections;

import org.bukkit.Material;

/** Immutable YAML-authored collection entry: one item a player can obtain. */
public record CollectionEntryDefinition(String id, String displayName, Material item, long rewardExperience) {
}
