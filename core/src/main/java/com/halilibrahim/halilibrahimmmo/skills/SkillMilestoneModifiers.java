package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Aggregated milestone bonuses summed across every configured skill's
 * currently-reached 25/50/75/100 tiers for one player. {@code hasteAmplifier}/
 * {@code speedAmplifier} are {@code -1} when no reached tier grants that
 * potion effect (the highest reached tier's amplifier wins, not a sum -
 * Haste I + Haste II should read as Haste II, not stack).
 */
public record SkillMilestoneModifiers(
        double maxHealthBonus,
        double pveDamageBonus,
        int maxEnergyBonus,
        int hasteAmplifier,
        int speedAmplifier
) {
    private static final SkillMilestoneModifiers NONE = new SkillMilestoneModifiers(0, 0, 0, -1, -1);

    public static SkillMilestoneModifiers none() {
        return NONE;
    }
}
