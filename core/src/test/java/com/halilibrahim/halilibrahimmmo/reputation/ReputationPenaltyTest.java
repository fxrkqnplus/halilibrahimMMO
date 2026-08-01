package com.halilibrahim.halilibrahimmmo.reputation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The death-penalty table, checked band by band.
 *
 * <p>Pure-logic, like {@code PrestigeRulesTest} and {@code UnlockConditionsTest}
 * - the whole point of keeping {@link ReputationPenalty} free of Bukkit is that
 * the numbers a player will argue about can be asserted without a server.</p>
 */
final class ReputationPenaltyTest {

    @Test
    @DisplayName("A player with nothing loses nothing, whatever kills them")
    void emptyTotalIsUntouchable() {
        for (ReputationDeathCause cause : ReputationDeathCause.values()) {
            assertEquals(0L, ReputationPenalty.lossFor(0L, cause));
            assertEquals(0L, ReputationPenalty.lossFor(-5L, cause));
        }
    }

    @Test
    @DisplayName("Below 1000, only another player costs anything")
    void lowBandsOnlyAnswerToPvp() {
        assertEquals(50L, ReputationPenalty.lossFor(50L, ReputationDeathCause.OYUNCU));
        assertEquals(0L, ReputationPenalty.lossFor(50L, ReputationDeathCause.YARATIK));
        assertEquals(0L, ReputationPenalty.lossFor(50L, ReputationDeathCause.KENDI));

        assertEquals(100L, ReputationPenalty.lossFor(999L, ReputationDeathCause.OYUNCU));
        assertEquals(0L, ReputationPenalty.lossFor(999L, ReputationDeathCause.YARATIK));
        assertEquals(0L, ReputationPenalty.lossFor(999L, ReputationDeathCause.KENDI));
    }

    @Test
    @DisplayName("Every three-way band charges self < creature < player")
    void middleBandsScaleByCause() {
        assertEquals(100L, ReputationPenalty.lossFor(1_000L, ReputationDeathCause.KENDI));
        assertEquals(250L, ReputationPenalty.lossFor(9_999L, ReputationDeathCause.YARATIK));
        assertEquals(500L, ReputationPenalty.lossFor(5_000L, ReputationDeathCause.OYUNCU));

        assertEquals(1_000L, ReputationPenalty.lossFor(10_000L, ReputationDeathCause.KENDI));
        assertEquals(2_500L, ReputationPenalty.lossFor(50_000L, ReputationDeathCause.YARATIK));
        assertEquals(5_000L, ReputationPenalty.lossFor(99_999L, ReputationDeathCause.OYUNCU));

        assertEquals(10_000L, ReputationPenalty.lossFor(100_000L, ReputationDeathCause.KENDI));
        assertEquals(25_000L, ReputationPenalty.lossFor(500_000L, ReputationDeathCause.YARATIK));
        assertEquals(50_000L, ReputationPenalty.lossFor(999_999L, ReputationDeathCause.OYUNCU));
    }

    @Test
    @DisplayName("The top band is open-ended, so passing ten million is not immunity")
    void topBandNeverRunsOut() {
        assertEquals(100_000L, ReputationPenalty.lossFor(1_000_000L, ReputationDeathCause.KENDI));
        assertEquals(250_000L, ReputationPenalty.lossFor(9_999_999L, ReputationDeathCause.YARATIK));
        assertEquals(500_000L, ReputationPenalty.lossFor(10_000_000L, ReputationDeathCause.OYUNCU));
        assertEquals(500_000L, ReputationPenalty.lossFor(900_000_000L, ReputationDeathCause.OYUNCU));
    }

    /**
     * The clamp only ever binds in the lowest band, where the penalty is
     * "everything" by definition - every higher band's charge is smaller than
     * its own floor. Asserted anyway, because that relationship is a property of
     * the numbers rather than of the code, and the next person to retune the
     * table is exactly who this test is for.
     */
    @Test
    @DisplayName("A loss never takes more than the player has")
    void lossIsClampedToTheTotal() {
        assertEquals(1L, ReputationPenalty.lossFor(1L, ReputationDeathCause.OYUNCU));
        assertEquals(99L, ReputationPenalty.lossFor(99L, ReputationDeathCause.OYUNCU));
        for (long total = 100L; total < 20_000_000L; total = total * 7 + 3) {
            for (ReputationDeathCause cause : ReputationDeathCause.values()) {
                assertEquals(ReputationPenalty.lossFor(total, cause),
                        Math.min(total, ReputationPenalty.lossFor(total, cause)),
                        "Loss exceeded the total at " + total + " for " + cause);
            }
        }
    }
}
