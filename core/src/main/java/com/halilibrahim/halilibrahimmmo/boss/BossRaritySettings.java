package com.halilibrahim.halilibrahimmmo.boss;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * Everything one rarity tier changes about a boss, straight from
 * {@code bosslar.yml}.
 *
 * <p>{@code weight} is a relative share, not a percentage: a rarity is picked
 * with probability {@code weight / (sum of all weights)}. Weights were chosen
 * over percentages deliberately, because a set of percentages that does not add
 * up to a hundred is a silent configuration bug, whereas any set of weights is
 * always a valid distribution.</p>
 */
public record BossRaritySettings(
        int weight,
        double healthMultiplier,
        double damageMultiplier,
        double speedMultiplier,
        long classExperience,
        int vanillaExperience,
        long wealthReward,
        boolean flees,
        boolean teleports,
        boolean drinksPotions,
        Map<BossWeaponType, Material> weapons,
        List<Material> armour,
        List<BossLootEntry> loot
) {
}
