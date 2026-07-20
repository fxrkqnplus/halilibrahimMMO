package com.halilibrahim.halilibrahimmmo.api.classes;

import java.util.Map;
import java.util.UUID;

/** Immutable read-only view of a player's class and shared talent budget. */
public record PlayerClassSnapshot(
        UUID playerId,
        String selectedClass,
        int availableTalentPoints,
        int spentTalentPoints,
        Map<String, Map<String, Integer>> talentRanks
) {
}
