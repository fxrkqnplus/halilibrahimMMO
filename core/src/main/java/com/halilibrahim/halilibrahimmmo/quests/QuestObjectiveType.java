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
    ITEM_OBTAIN
}
