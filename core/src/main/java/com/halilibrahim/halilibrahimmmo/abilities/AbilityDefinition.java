package com.halilibrahim.halilibrahimmmo.abilities;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Immutable configured ability definition. {@code limit} and
 * {@code damageMultiplier} are dual-purposed per {@code effectType} rather
 * than growing this record further: {@code limit} is a block count for
 * {@link AbilityEffectType#EXCAVATE}/{@link AbilityEffectType#TREE_FELL}/
 * {@link AbilityEffectType#VEIN_MINE} and a radius in blocks for
 * {@link AbilityEffectType#KNOCKBACK_SLAM}/{@link AbilityEffectType#ORE_REVEAL};
 * {@code damageMultiplier} is a damage multiplier for
 * {@link AbilityEffectType#DAMAGE_BUFF}, a 0-1 chance for
 * {@link AbilityEffectType#EXTRA_DROP_CHANCE} and a 0-1 minimum open-air ratio
 * for {@link AbilityEffectType#ORE_REVEAL}'s "wide mine" precondition.
 *
 * <p>{@code energyPerHit} follows the same convention: it is the per-swing
 * energy drain of an {@link AbilityActivationMode#ENERJI} damage buff, and the
 * price of restoring one heart to a summoned guardian for
 * {@link AbilityEffectType#GUARDIAN_CALL}. Both answer the same question - what
 * one unit of this ability's combat effect costs the player in energy.
 *
 * <p>{@code energyPerBlock} is the per-block running cost of a sweep ability
 * ({@code EXCAVATE}/{@code TREE_FELL}/{@code VEIN_MINE}): energy is spent as
 * the sweep progresses and the sweep stops the moment the player runs out, so
 * "how much can I fell in one go" is answered by the energy bar rather than by
 * a flat block limit alone. {@code 0} restores the older behaviour of paying
 * only {@code energyCost} up front.</p>
 */
public record AbilityDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        NamespacedKey model,
        String skillId,
        int requiredLevel,
        int energyCost,
        long cooldownMillis,
        long activeMillis,
        AbilityEffectType effectType,
        int limit,
        double damageMultiplier,
        AbilityToolTag tool,
        String classId,
        AbilityActivationMode mode,
        int energyPerHit,
        int energyPerBlock,
        AbilityVisual visual
) {
}
