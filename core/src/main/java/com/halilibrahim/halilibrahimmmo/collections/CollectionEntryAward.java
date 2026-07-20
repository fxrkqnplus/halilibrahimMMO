package com.halilibrahim.halilibrahimmmo.collections;

/** Result of successfully obtaining one new collection entry, including any completion bonus. */
public record CollectionEntryAward(
        String collectionId,
        String collectionDisplayName,
        String entryId,
        String entryDisplayName,
        long entryExperience,
        boolean collectionCompleted,
        long completionExperience
) {
}
