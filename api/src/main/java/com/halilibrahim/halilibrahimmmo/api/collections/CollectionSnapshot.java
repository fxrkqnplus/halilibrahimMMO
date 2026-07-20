package com.halilibrahim.halilibrahimmmo.api.collections;

import java.util.UUID;

/** Immutable, external-safe view of one player's progress toward a configured collection. */
public record CollectionSnapshot(UUID playerId, String collectionId, int obtainedCount, int totalEntries, boolean completed) {
}
