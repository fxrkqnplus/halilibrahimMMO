package com.halilibrahim.halilibrahimmmo.cosmetics;

import com.halilibrahim.halilibrahimmmo.unlock.UnlockCondition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;

/** Immutable YAML-authored cosmetic definition. */
public record CosmeticDefinition(String id, String displayName, Material icon, NamespacedKey model,
                                 Particle effect, UnlockCondition condition) {
}
