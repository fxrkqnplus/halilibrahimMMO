package com.halilibrahim.halilibrahimmmo.skills;

/**
 * One skill's configured effect at one milestone tier level (25/50/75/100).
 * {@code value} is the effect's own unit - health points for
 * {@code MAX_HEALTH}, a fractional multiplier for
 * {@code PVE_DAMAGE_MULTIPLIER}, energy points for {@code MAX_ENERGY}, or a
 * potion-effect amplifier (0 = tier I, 1 = tier II, ...) for
 * {@code HASTE}/{@code SPEED}.
 */
public record SkillMilestoneEffect(SkillMilestoneEffectType type, double value) {
}
