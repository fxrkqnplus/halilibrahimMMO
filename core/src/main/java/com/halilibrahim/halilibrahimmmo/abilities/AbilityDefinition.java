package com.halilibrahim.halilibrahimmmo.abilities;

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
 */
public record AbilityDefinition(
        String id,
        String displayName,
        String description,
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
        AbilityVisual visual
) {
}
