package com.halilibrahim.halilibrahimmmo.skills;

/**
 * Aggregated milestone bonuses summed across every configured skill's
 * currently-reached 25/50/75/100 tiers for one player. {@code hasteAmplifier}/
 * {@code speedAmplifier} are {@code -1} when no reached tier grants that
 * potion effect (the highest reached tier's amplifier wins, not a sum -
 * Haste I + Haste II should read as Haste II, not stack).
 *
 * <p>{@code treeFellingLimit}, {@code breedRefundChance} and
 * {@code breedTwinChance} ({@code 0} = not unlocked) are unlocked
 * <em>features</em> rather than stat boosts: nothing applies them to the
 * player, the odunculuk block-break path and the hayvancılık breeding path
 * read them at the moment they fire. The highest reached tier wins for each,
 * since two tiers of the same feature describe one capability at different
 * strengths, not two stacking bonuses. They can live on this cross-skill
 * aggregate only because exactly one skill declares each - the milestone
 * features that several skills share are read per skill instead, through
 * {@link SkillService#milestoneValue}.</p>
 */
public record SkillMilestoneModifiers(
        double maxHealthBonus,
        double pveDamageBonus,
        int maxEnergyBonus,
        int hasteAmplifier,
        int speedAmplifier,
        int treeFellingLimit,
        double breedRefundChance,
        double breedTwinChance
) {
    private static final SkillMilestoneModifiers NONE = new SkillMilestoneModifiers(0, 0, 0, -1, -1, 0, 0, 0);

    public static SkillMilestoneModifiers none() {
        return NONE;
    }
}
