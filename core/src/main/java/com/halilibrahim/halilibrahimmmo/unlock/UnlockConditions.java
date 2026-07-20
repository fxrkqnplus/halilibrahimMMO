package com.halilibrahim.halilibrahimmmo.unlock;

/** Pure evaluation of one unlock condition against one player's current state. */
public final class UnlockConditions {

    private UnlockConditions() {
        throw new UnsupportedOperationException("UnlockConditions is a static utility class");
    }

    public static boolean isSatisfied(UnlockCondition condition, UnlockContext context) {
        return switch (condition.type()) {
            case LEVEL -> context.globalLevel() >= condition.level();
            case PRESTIGE -> context.prestige() >= condition.level();
            case QUEST_COMPLETED -> context.completedQuestIds().contains(condition.referenceId());
            case COLLECTION_COMPLETED -> context.completedCollectionIds().contains(condition.referenceId());
            case SKILL_LEVEL -> context.skillLevels().getOrDefault(condition.referenceId(), 0) >= condition.level();
        };
    }
}
