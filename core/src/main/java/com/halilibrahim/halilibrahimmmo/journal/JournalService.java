package com.halilibrahim.halilibrahimmmo.journal;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Append-only, asynchronous record of a player's quest and collection milestones. */
public interface JournalService extends PluginService {

    CompletableFuture<Void> recordQuestCompleted(UUID playerId, String questId, String questDisplayName, long experienceAwarded);

    CompletableFuture<Void> recordCollectionEntryObtained(UUID playerId, String entryId, String entryDisplayName, String collectionDisplayName, long experienceAwarded);

    CompletableFuture<Void> recordCollectionCompleted(UUID playerId, String collectionId, String collectionDisplayName, long experienceAwarded);

    CompletableFuture<List<JournalEntry>> recent(UUID playerId, int limit);
}
