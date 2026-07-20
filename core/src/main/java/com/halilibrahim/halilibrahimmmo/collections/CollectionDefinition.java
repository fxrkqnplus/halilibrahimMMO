package com.halilibrahim.halilibrahimmmo.collections;

import java.util.Map;

/** Immutable YAML-authored collection: a checklist of entries plus a completion bonus. */
public record CollectionDefinition(String id, String displayName, Map<String, CollectionEntryDefinition> entries, long completionRewardExperience) {
}
