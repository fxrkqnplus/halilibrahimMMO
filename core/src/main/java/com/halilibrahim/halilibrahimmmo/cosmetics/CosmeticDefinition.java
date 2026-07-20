package com.halilibrahim.halilibrahimmmo.cosmetics;

import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import org.bukkit.Material;
import org.bukkit.Particle;

/** Immutable YAML-authored cosmetic definition. */
public record CosmeticDefinition(String id, String displayName, Material icon, Particle effect, UnlockCondition condition) {
}
