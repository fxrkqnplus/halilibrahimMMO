package com.halilibrahim.halilibrahimmmo.classes;

/** Immutable configured definition of one rankable class-talent branch. */
public record TalentNodeDefinition(
        String id,
        String displayName,
        int maximumRank,
        ClassEffectType effectType,
        String abilityId,
        double valuePerRank
) {
}
