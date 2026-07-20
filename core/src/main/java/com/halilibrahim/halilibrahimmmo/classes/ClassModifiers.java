package com.halilibrahim.halilibrahimmmo.classes;

/** Aggregated, selected-class-only modifiers used by energy and ability services. */
public record ClassModifiers(
        double maximumHealthBonus,
        double pveDamageBonus,
        int maximumEnergyBonus,
        java.util.Map<String, Double> energyCostReductions,
        java.util.Map<String, Double> cooldownReductions,
        java.util.Map<String, Integer> limitBonuses
) {
    private static final ClassModifiers NONE = new ClassModifiers(0, 0, 0, java.util.Map.of(), java.util.Map.of(), java.util.Map.of());

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
}
