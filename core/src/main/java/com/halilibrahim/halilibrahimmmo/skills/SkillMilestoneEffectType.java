package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Bounded set of effect types a skill's 25/50/75/100 milestone tier can grant
 * (Phase 12 - each skill is differentiated instead of sharing one flat
 * MAX_HEALTH formula). Deliberately a separate enum from
 * {@code classes.ClassEffectType} - that enum also carries ability-scoped
 * members (energy cost/cooldown/limit reductions) that make no sense for a
 * skill milestone, which has no associated ability id.
 *
 * <p>{@link #TREE_FELLING} and {@link #BREED_REFUND} are Phase 15's two
 * <em>unlocked feature</em> milestones: unlike the five stat boosts above them
 * they are never applied to the player, they are queried at the moment the
 * relevant gameplay event fires (see
 * {@link SkillMilestoneModifiers#treeFellingLimit()} /
 * {@link SkillMilestoneModifiers#breedRefundChance()}). Reusing the milestone
 * mechanism instead of inventing a bespoke per-skill config block keeps "what
 * does level 25 give me" one uniform question across all six skills.</p>
 */
public enum SkillMilestoneEffectType {
    MAX_HEALTH,
    PVE_DAMAGE_MULTIPLIER,
    MAX_ENERGY,
    HASTE,
    SPEED,
    /** Unlocks whole-tree felling when a tree's root log is broken; {@code value} is the maximum log count. */
    TREE_FELLING,
    /** Unlocks the chance of getting the breeding food back after a baby is born; {@code value} is a 0-1 chance. */
    BREED_REFUND
}
