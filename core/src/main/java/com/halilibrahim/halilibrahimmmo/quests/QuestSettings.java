package com.halilibrahim.halilibrahimmmo.quests;

import java.util.Map;

/**
 * Complete immutable quests.yml snapshot. {@code dailyAssignedCount}/
 * {@code weeklyAssignedCount} are how many quests get randomly picked from
 * the DAILY/WEEKLY pools per period - PERMANENT quests are never pooled.
 */
public record QuestSettings(Map<String, QuestDefinition> definitions, int dailyAssignedCount, int weeklyAssignedCount) {
    public int assignedCount(QuestType type) {
        return switch (type) {
            case DAILY -> dailyAssignedCount;
            case WEEKLY -> weeklyAssignedCount;
            case PERMANENT -> throw new IllegalArgumentException("PERMANENT quests are never pooled.");
        };
    }
}
