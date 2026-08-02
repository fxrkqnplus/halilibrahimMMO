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
 *
 * @param intelligence          which behaviours this tier's brain is allowed to
 *                              use. One named level rather than a switch per
 *                              capability - see {@link BossIntelligence}.
 * @param healthRegenPerSecond  how much of its own maximum health a recovering
 *                              boss mends each second, as a ratio. Expressed
 *                              relative to its own maximum so a Mistik's
 *                              recovery takes about as long as an ordinary
 *                              boss's, instead of a flat rate that would be
 *                              instantaneous on one and hopeless on the other.
 * @param fleeRatio             the health fraction at which it decides to run
 * @param recoveryRatio         the health fraction it mends back up to before
 *                              returning. Deliberately below full: a boss that
 *                              comes back untouched has erased the fight rather
 *                              than survived it.
 * @param escapes               how many times over its whole life it may run.
 *                              Zero means it fights where it stands.
 */
public record BossRaritySettings(
        int weight,
        double healthMultiplier,
        double damageMultiplier,
        double speedMultiplier,
        long classExperience,
        int vanillaExperience,
        long reputationReward,
        BossIntelligence intelligence,
        double healthRegenPerSecond,
        double fleeRatio,
        double recoveryRatio,
        int escapes,
        boolean teleports,
        Map<BossWeaponType, Material> weapons,
        List<Material> armour,
        List<BossLootEntry> loot
) {
}
