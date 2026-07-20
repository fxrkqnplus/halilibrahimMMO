package com.halilibrahim.halilibrahimmmo.classes;

import java.util.Map;
import java.util.UUID;

/** Persistent player talent allocation, kept independently from the selected class. */
public record PlayerClassProgress(UUID playerId, Map<String, Map<String, Integer>> talentRanks, long classChangeReadyAtMillis) {
    public int spentPoints() {
        return talentRanks.values().stream().flatMap(nodes -> nodes.values().stream()).mapToInt(Integer::intValue).sum();
    }
}
