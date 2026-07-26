package com.halilibrahim.halilibrahimmmo.classes;

/**
 * The two independent talent-point cadences parsed from {@code classes.yml}'s
 * {@code yetenek-puani} section: how much Güç buys one point of each kind, and
 * how many of each a prestige rank grants outright.
 */
public record TalentPointSettings(
        int levelsPerFeaturePoint,
        int levelsPerAbilityPoint,
        int featurePointsPerPrestige,
        int abilityPointsPerPrestige
) {
}
