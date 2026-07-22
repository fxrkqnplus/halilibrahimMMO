package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Bounded set of effect types a skill's 25/50/75/100 milestone tier can grant
 * (Phase 12 - each skill is differentiated instead of sharing one flat
 * MAX_HEALTH formula). Deliberately a separate enum from
 * {@code classes.ClassEffectType} - that enum also carries ability-scoped
 * members (energy cost/cooldown/limit reductions) that make no sense for a
 * skill milestone, which has no associated ability id.
 */
public enum SkillMilestoneEffectType {
    MAX_HEALTH,
    PVE_DAMAGE_MULTIPLIER,
    MAX_ENERGY,
    HASTE,
    SPEED
}
