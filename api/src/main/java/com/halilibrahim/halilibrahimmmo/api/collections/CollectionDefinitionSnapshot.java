package com.halilibrahim.halilibrahimmmo.api.collections;

/** Immutable, external-safe view of one configured collection. */
public record CollectionDefinitionSnapshot(String id, String displayName, int totalEntries) {
}
