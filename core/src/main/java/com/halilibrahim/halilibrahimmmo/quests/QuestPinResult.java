package com.halilibrahim.halilibrahimmmo.quests;

/** What happened when a player clicked a quest's pin toggle on the quest board. */
public enum QuestPinResult {
    PINNED,
    UNPINNED,
    /** This section already holds {@link QuestService#MAXIMUM_PINS_PER_TYPE} pins. */
    FULL,
    UNKNOWN_QUEST
}
