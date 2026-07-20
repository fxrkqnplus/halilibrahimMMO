package com.halilibrahim.halilibrahimmmo.api.quests;

/** Immutable, external-safe view of one configured quest. */
public record QuestDefinitionSnapshot(String id, String displayName, int target) {
}
