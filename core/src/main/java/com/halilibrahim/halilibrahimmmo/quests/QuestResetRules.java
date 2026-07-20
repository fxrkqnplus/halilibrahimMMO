package com.halilibrahim.halilibrahimmmo.quests;

/** Pure daily/weekly reset-window arithmetic, kept separate from persistence and caching. */
public final class QuestResetRules {

    private static final long DAY_MILLIS = 86_400_000L;
    private static final long WEEK_MILLIS = 7 * DAY_MILLIS;

    private QuestResetRules() {
        throw new UnsupportedOperationException("QuestResetRules is a static utility class");
    }

    public static long periodLengthMillis(QuestType type) {
        return switch (type) {
            case PERMANENT -> Long.MAX_VALUE;
            case DAILY -> DAY_MILLIS;
            case WEEKLY -> WEEK_MILLIS;
        };
    }

    /** @return {@code true} once {@code nowMillis} has reached a repeating quest's period end. */
    public static boolean isExpired(QuestType type, long periodEndMillis, long nowMillis) {
        return type != QuestType.PERMANENT && nowMillis >= periodEndMillis;
    }

    /** @return the epoch millis at which the period starting at {@code nowMillis} ends. */
    public static long nextPeriodEnd(QuestType type, long nowMillis) {
        return type == QuestType.PERMANENT ? Long.MAX_VALUE : Math.addExact(nowMillis, periodLengthMillis(type));
    }
}
