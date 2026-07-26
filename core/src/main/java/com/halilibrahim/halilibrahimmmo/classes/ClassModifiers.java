package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;

/** Aggregated, selected-class-only modifiers used by energy and ability services. */
public record ClassModifiers(
        double maximumHealthBonus,
        double pveDamageBonus,
        int maximumEnergyBonus,
        Map<String, Double> energyCostReductions,
        Map<String, Double> cooldownReductions,
        Map<String, Integer> limitBonuses,
        Map<String, Double> durationBonuses,
        Map<String, Double> effectBonuses
) {
    private static final ClassModifiers NONE =
            new ClassModifiers(0, 0, 0, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

    public static ClassModifiers none() {
        return NONE;
    }

    public int adjustedEnergyCost(String abilityId, int base) {
        if (base == 0) return 0;
        return Math.max(1, (int) Math.ceil(base * (1.0D - energyCostReductions.getOrDefault(abilityId, 0.0D))));
    }

    public long adjustedCooldownMillis(String abilityId, long base) {
        return Math.max(0L, (long) Math.ceil(base * (1.0D - cooldownReductions.getOrDefault(abilityId, 0.0D))));
    }

    public int adjustedLimit(String abilityId, int base) {
        return Math.addExact(base, limitBonuses.getOrDefault(abilityId, 0));
    }

    /** Talent-extended active duration - never shorter than the configured one. */
    public long adjustedDurationMillis(String abilityId, long base) {
        return Math.round(base * (1.0D + durationBonuses.getOrDefault(abilityId, 0.0D)));
    }

    /**
     * Talent-strengthened effect scalar. Deliberately not clamped: what the
     * scalar means depends on the effect type (a damage multiplier, a raw
     * damage number, a 0-1 chance), and the one case where exceeding 1 would
     * be meaningless - a drop chance - simply always succeeds, which is
     * exactly what a fully-ranked chance talent should do.
     */
    public double adjustedEffectValue(String abilityId, double base) {
        return base * (1.0D + effectBonuses.getOrDefault(abilityId, 0.0D));
    }
}
