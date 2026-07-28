package com.halilibrahim.halilibrahimmmo.unlock;

/**
 * One YAML-authored unlock condition. {@code level} carries the LEVEL/
 * SKILL_LEVEL/PRESTIGE threshold; {@code referenceId} carries the quest/
 * collection/skill id; {@code amount} carries the WEALTH threshold, which needs
 * a {@code long} because Zenginlik Puanı totals run past what an {@code int}
 * holds. Only the field(s) relevant to {@code type} are populated - the others
 * are left at their zero value / empty string.
 */
public record UnlockCondition(UnlockConditionType type, int level, String referenceId, long amount) {
}
