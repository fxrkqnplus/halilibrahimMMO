package com.halilibrahim.halilibrahimmmo.api.quests;

import java.util.UUID;

/** Immutable, external-safe view of one player's progress toward a configured quest. */
public record QuestSnapshot(UUID playerId, String questId, int progress, boolean completed) {
}
