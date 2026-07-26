package com.halilibrahim.halilibrahimmmo.classes;

/**
 * The bounded set of class-talent effects supported by the gameplay engine.
 * Each member also declares which {@link TalentPointCategory} pool a talent
 * granting it is paid for out of - see that enum for why the category is
 * derived here instead of being a separate {@code classes.yml} key.
 */
public enum ClassEffectType {
    MAX_HEALTH(TalentPointCategory.OZELLIK),
    PVE_DAMAGE_MULTIPLIER(TalentPointCategory.OZELLIK),
    MAX_ENERGY(TalentPointCategory.OZELLIK),
    ABILITY_ENERGY_COST_REDUCTION(TalentPointCategory.KABILIYET),
    ABILITY_COOLDOWN_REDUCTION(TalentPointCategory.KABILIYET),
    ABILITY_LIMIT_BONUS(TalentPointCategory.KABILIYET),
    /** Extends one ability's active duration by a fraction of its configured length. */
    ABILITY_DURATION_BONUS(TalentPointCategory.KABILIYET),
    /**
     * Strengthens one ability's own effect scalar by a fraction: the damage
     * multiplier of a {@code DAMAGE_BUFF}, the drop chance of an
     * {@code EXTRA_DROP_CHANCE}, the raw damage of a {@code KNOCKBACK_SLAM}.
     * Abilities whose effect carries no such scalar are simply unaffected.
     */
    ABILITY_EFFECT_BONUS(TalentPointCategory.KABILIYET);

    private final TalentPointCategory category;

    ClassEffectType(TalentPointCategory category) {
        this.category = category;
    }

    public TalentPointCategory category() {
        return category;
    }

    /** Whether this effect is scoped to one named ability and therefore requires an ability id. */
    public boolean requiresAbility() {
        return category == TalentPointCategory.KABILIYET;
    }
}
