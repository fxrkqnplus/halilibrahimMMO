package com.halilibrahim.halilibrahimmmo.locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-logic coverage for the compact number rendering, in the same shape as
 * {@link DurationFormatterTest} - no Bukkit, no server, no configuration.
 */
final class NumberFormatterTest {

    @Test
    @DisplayName("Under a thousand stays exact")
    void smallValuesAreNotAbbreviated() {
        assertEquals("0", NumberFormatter.compact(0));
        assertEquals("7", NumberFormatter.compact(7));
        assertEquals("999", NumberFormatter.compact(999));
    }

    @Test
    @DisplayName("Thousands keep one decimal and drop a trailing zero")
    void thousandsUseOneDecimal() {
        assertEquals("1K", NumberFormatter.compact(1_000));
        assertEquals("12.6K", NumberFormatter.compact(12_600));
        assertEquals("712.2K", NumberFormatter.compact(712_200));
    }

    @Test
    @DisplayName("Millions and billions keep up to two decimals")
    void largeValuesUseTwoDecimals() {
        assertEquals("1.2M", NumberFormatter.compact(1_200_000));
        assertEquals("1.05M", NumberFormatter.compact(1_050_000));
        assertEquals("2.5B", NumberFormatter.compact(2_500_000_000L));
    }

    @Test
    @DisplayName("A value that rounds onto its unit's ceiling is promoted, never rendered as 1000K")
    void roundingNeverOverflowsItsOwnUnit() {
        assertEquals("1M", NumberFormatter.compact(999_990));
        assertEquals("1B", NumberFormatter.compact(999_999_999));
    }

    @Test
    @DisplayName("Negative totals cannot occur but still render sanely")
    void negativeValuesKeepTheirSign() {
        assertEquals("-12.6K", NumberFormatter.compact(-12_600));
    }
}
