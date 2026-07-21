package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;
import java.util.UUID;

/**
 * Persistent player talent allocation and per-class level track, kept
 * independently from the currently selected class - switching classes never
 * discards either a class's spent talent ranks or its accumulated level/XP.
 */
public record PlayerClassProgress(
        UUID playerId,
        Map<String, Map<String, Integer>> talentRanks,
        long classChangeReadyAtMillis,
        Map<String, ClassLevelProgress> classLevels
) {
    public int spentPoints() {
        return talentRanks.values().stream().flatMap(nodes -> nodes.values().stream()).mapToInt(Integer::intValue).sum();
    }

    public ClassLevelProgress levelProgress(String classId) {
        return classLevels.getOrDefault(classId, new ClassLevelProgress(classId, 1, 0L));
    }
}
