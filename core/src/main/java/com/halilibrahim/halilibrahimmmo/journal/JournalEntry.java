package com.halilibrahim.halilibrahimmmo.journal;

import java.util.UUID;

/**
 * One immutable, timestamped adventure-journal record. {@code referenceName} is
 * captured at write time so a player's history stays readable even if the
 * underlying quest or collection is later renamed or removed from configuration.
 */
public record JournalEntry(long id, UUID playerId, JournalEntryType type, String referenceId, String referenceName, long experienceAwarded, long createdAtMillis) {
}
