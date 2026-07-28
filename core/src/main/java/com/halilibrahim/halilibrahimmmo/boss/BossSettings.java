package com.halilibrahim.halilibrahimmmo.boss;

import java.util.List;
import java.util.Map;

/** The immutable {@code bosslar.yml} snapshot. */
public record BossSettings(
        long spawnIntervalMillis,
        int minimumDistance,
        int maximumDistance,
        Map<BossRarity, BossRaritySettings> rarities,
        Map<String, BossArchetype> archetypes
) {

    /** Total of every rarity's weight - the denominator of the weighted pick. */
    public int totalWeight() {
        return rarities.values().stream().mapToInt(BossRaritySettings::weight).sum();
    }

    public List<BossArchetype> archetypeList() {
        return List.copyOf(archetypes.values());
    }
}
