package com.halilibrahim.halilibrahimmmo.reputation;

/**
 * What a death costs in Saygınlık.
 *
 * <p>Pure and dependency-free, in the same mould as {@code PrestigeRules} and
 * {@code QuestResetRules}, so the whole table can be read - and tested - without
 * a server anywhere near it.</p>
 *
 * <p><strong>The band is chosen by the total the player had before dying</strong>,
 * and the loss grows with it. That is the point of the table: at fifty thousand
 * points a five-thousand-point death is an afternoon, and at fifty points it
 * would be everything, so the same flat number cannot serve both. Two deliberate
 * readings of the brief are recorded here because neither is spelled out in
 * it:</p>
 *
 * <ul>
 *   <li><strong>Below a thousand points, only another player can cost you
 *   anything.</strong> The brief names a penalty for those two bands only for a
 *   player kill, and that is also the only reading that makes sense: a new
 *   player who drowns learning the game has nothing to lose and losing it would
 *   teach them nothing.</li>
 *   <li><strong>The top band is open-ended.</strong> The brief stops at ten
 *   million; somebody will pass it, and a player who does must not silently stop
 *   being able to lose anything at all.</li>
 * </ul>
 *
 * <p>A loss is always clamped to what the player actually has, so a total can
 * reach zero but never go below it.</p>
 */
public final class ReputationPenalty {

    private ReputationPenalty() {
        throw new UnsupportedOperationException("ReputationPenalty is a static utility class");
    }

    /**
     * How many points this death takes off {@code current}.
     *
     * @return a non-negative amount, never larger than {@code current}
     */
    public static long lossFor(long current, ReputationDeathCause cause) {
        if (current <= 0) {
            return 0L;
        }
        long raw = rawLoss(current, cause);
        return Math.min(current, raw);
    }

    private static long rawLoss(long current, ReputationDeathCause cause) {
        if (current < 100L) {
            // Everything, and only to another player. There is no smaller
            // number that would mean anything at this scale.
            return cause == ReputationDeathCause.OYUNCU ? current : 0L;
        }
        if (current < 1_000L) {
            return cause == ReputationDeathCause.OYUNCU ? 100L : 0L;
        }
        if (current < 10_000L) {
            return switch (cause) {
                case KENDI -> 100L;
                case YARATIK -> 250L;
                case OYUNCU -> 500L;
            };
        }
        if (current < 100_000L) {
            return switch (cause) {
                case KENDI -> 1_000L;
                case YARATIK -> 2_500L;
                case OYUNCU -> 5_000L;
            };
        }
        if (current < 1_000_000L) {
            return switch (cause) {
                case KENDI -> 10_000L;
                case YARATIK -> 25_000L;
                case OYUNCU -> 50_000L;
            };
        }
        return switch (cause) {
            case KENDI -> 100_000L;
            case YARATIK -> 250_000L;
            case OYUNCU -> 500_000L;
        };
    }
}
