package com.halilibrahim.halilibrahimmmo.quests;

/** Gameplay event category that can advance a quest's progress. */
public enum QuestObjectiveType {
    BLOCK_BREAK,
    MOB_KILL,
    /**
     * Standing inside one of the quest's configured world structures. Credited
     * once per structure kind per player, not once per step taken inside it -
     * see {@code StructureDiscoveryListener}.
     */
    STRUCTURE_DISCOVER,
    /** Obtaining one of the quest's configured items, off the ground or out of a container. */
    ITEM_OBTAIN,
    /**
     * Performing one specific, named deed - see {@link QuestDeed}. For the
     * objectives that are a story rather than a counter: killing a ghast with
     * its own fireball, bartering with a piglin, landing a fish.
     */
    DEED
}
