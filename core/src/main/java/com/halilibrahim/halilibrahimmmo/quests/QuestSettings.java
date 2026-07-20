package com.halilibrahim.halilibrahimmmo.quests;

import java.util.Map;

/** Complete immutable quests.yml snapshot. */
public record QuestSettings(Map<String, QuestDefinition> definitions) {
}
