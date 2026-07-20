package com.halilibrahim.halilibrahimmmo.journal;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async, database-backed adventure journal. Has no YAML settings of its own, so
 * {@link #reload()} stays the {@code PluginService} default no-op.
 */
public final class DatabaseJournalService implements JournalService {

    private final JournalRepository repository;

    public DatabaseJournalService(JournalRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }

    @Override
    public CompletableFuture<Void> recordQuestCompleted(UUID playerId, String questId, String questDisplayName, long experienceAwarded) {
        return repository.insert(playerId, JournalEntryType.QUEST_COMPLETED, questId, questDisplayName, experienceAwarded);
    }

    @Override
    public CompletableFuture<Void> recordCollectionEntryObtained(UUID playerId, String entryId, String entryDisplayName, String collectionDisplayName, long experienceAwarded) {
        return repository.insert(playerId, JournalEntryType.COLLECTION_ENTRY_OBTAINED, entryId, entryDisplayName + " (" + collectionDisplayName + ")", experienceAwarded);
    }

    @Override
    public CompletableFuture<Void> recordCollectionCompleted(UUID playerId, String collectionId, String collectionDisplayName, long experienceAwarded) {
        return repository.insert(playerId, JournalEntryType.COLLECTION_COMPLETED, collectionId, collectionDisplayName, experienceAwarded);
    }

    @Override
    public CompletableFuture<List<JournalEntry>> recent(UUID playerId, int limit) {
        return repository.findRecent(playerId, limit);
    }
}
