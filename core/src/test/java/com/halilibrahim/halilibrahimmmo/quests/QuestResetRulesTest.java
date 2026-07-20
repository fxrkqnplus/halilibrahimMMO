package com.halilibrahim.halilibrahimmmo.quests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QuestResetRulesTest {

    @Test void permanentQuestsNeverExpire() {
        assertFalse(QuestResetRules.isExpired(QuestType.PERMANENT, 0L, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, QuestResetRules.nextPeriodEnd(QuestType.PERMANENT, 1_000L));
    }

    @Test void dailyQuestsExpireExactlyOneDayAfterTheirPeriodEnd() {
        long now = 10_000L;
        long periodEnd = QuestResetRules.nextPeriodEnd(QuestType.DAILY, now);
        assertEquals(now + 86_400_000L, periodEnd);
        assertFalse(QuestResetRules.isExpired(QuestType.DAILY, periodEnd, periodEnd - 1));
        assertTrue(QuestResetRules.isExpired(QuestType.DAILY, periodEnd, periodEnd));
    }

    @Test void weeklyQuestsSpanSevenDays() {
        assertEquals(7 * 86_400_000L, QuestResetRules.nextPeriodEnd(QuestType.WEEKLY, 0L));
    }
}
