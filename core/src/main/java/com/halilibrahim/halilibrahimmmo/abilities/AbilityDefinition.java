package com.halilibrahim.halilibrahimmmo.abilities;

/** Immutable configured ability definition. */
public record AbilityDefinition(
        String id,
        String displayName,
        String skillId,
        int requiredLevel,
        int energyCost,
        long cooldownMillis,
        long activeMillis,
        AbilityEffectType effectType,
        int limit,
        double damageMultiplier
) {
}
