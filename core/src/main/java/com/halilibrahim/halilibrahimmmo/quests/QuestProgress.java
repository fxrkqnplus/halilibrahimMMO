package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.api.quests.QuestSnapshot;

import java.util.UUID;

/** Immutable internal quest state at the cache and repository boundary. */
public record QuestProgress(UUID playerId, String questId, int progress, boolean completed, long periodEndMillis) {

    public QuestSnapshot snapshot() {
        return new QuestSnapshot(playerId, questId, progress, completed);
    }
}
