package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.api.collections.CollectionSnapshot;

import java.util.Set;
import java.util.UUID;

/** Immutable internal collection state at the cache and repository boundary. */
public record CollectionProgress(UUID playerId, String collectionId, Set<String> obtainedEntryIds) {

    public int obtainedCount() {
        return obtainedEntryIds.size();
    }

    public boolean completed(CollectionDefinition definition) {
        return obtainedEntryIds.containsAll(definition.entries().keySet());
    }

    public CollectionSnapshot snapshot(CollectionDefinition definition) {
        int total = definition.entries().size();
        return new CollectionSnapshot(playerId, collectionId, obtainedEntryIds.size(), total, completed(definition));
    }
}
