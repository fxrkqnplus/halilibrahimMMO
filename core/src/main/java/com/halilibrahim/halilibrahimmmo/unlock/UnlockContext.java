package com.halilibrahim.halilibrahimmmo.unlock;

import java.util.Map;
import java.util.Set;

/** One player's current state, snapshotted just before evaluating every unlock condition against it. */
public record UnlockContext(
        int globalLevel,
        int prestige,
        Set<String> completedQuestIds,
        Set<String> completedCollectionIds,
        Map<String, Integer> skillLevels,
        long reputation
) {
}
