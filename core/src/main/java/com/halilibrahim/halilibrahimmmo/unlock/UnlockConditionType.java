package com.halilibrahim.halilibrahimmmo.unlock;

/** Milestone category a title/cosmetic unlock condition can be checked against. */
public enum UnlockConditionType {
    LEVEL,
    PRESTIGE,
    QUEST_COMPLETED,
    COLLECTION_COMPLETED,
    SKILL_LEVEL,
    /** A Zenginlik Puanı total - the one condition that rewards what a player has taken out of the world, not how far they levelled. */
    WEALTH
}
