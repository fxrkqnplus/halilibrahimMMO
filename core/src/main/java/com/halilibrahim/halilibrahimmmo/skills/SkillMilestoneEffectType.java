package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Bounded set of effect types a skill's 25/50/75/100 milestone tier can grant.
 * Deliberately a separate enum from {@code classes.ClassEffectType} - that enum
 * also carries ability-scoped members (energy cost/cooldown/limit reductions)
 * that make no sense for a skill milestone, which has no associated ability id.
 *
 * <p><strong>Members fall into three groups, and which group a member is in
 * decides how it is read back.</strong></p>
 *
 * <ol>
 *   <li><em>Player stats</em> ({@link #MAX_HEALTH}, {@link #PVE_DAMAGE_MULTIPLIER},
 *   {@link #MAX_ENERGY}, {@link #HASTE}, {@link #SPEED}) are applied to the
 *   player and summed across every skill, through
 *   {@link SkillService#milestoneModifiers(java.util.UUID)}.</li>
 *   <li><em>Single-skill features</em> ({@link #TREE_FELLING},
 *   {@link #BREED_REFUND}, {@link #BREED_TWIN}) are never applied to anything;
 *   the one gameplay path that cares reads them off the same aggregate, which
 *   is safe precisely because exactly one skill declares each.</li>
 *   <li><em>Per-skill features</em> ({@link #DOUBLE_DROP}, {@link #EXCAVATION},
 *   {@link #AUTO_REPLANT}) are scoped to the blocks of the skill that granted
 *   them, and are therefore read through
 *   {@link SkillService#milestoneValue(java.util.UUID, String, SkillMilestoneEffectType)}
 *   instead. Several skills grant {@code DOUBLE_DROP} at different strengths,
 *   so folding them into one number would let a maxed odunculuk double a
 *   diamond ore.</li>
 * </ol>
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
    BREED_REFUND,
    /** Chance for one breeding to produce a second baby; {@code value} is a 0-1 chance. */
    BREED_TWIN,
    /** Chance for one of <em>this skill's own</em> blocks to drop twice; {@code value} is a 0-1 chance. */
    DOUBLE_DROP,
    /** Breaking one of this skill's own blocks also clears the 3x3 around it; {@code value} is the maximum extra blocks. */
    EXCAVATION,
    /** Chance for a fully-grown crop this skill owns to replant itself; {@code value} is a 0-1 chance. */
    AUTO_REPLANT
}
