package com.halilibrahim.halilibrahimmmo.locale;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Renders large counters in a compact, glanceable form: {@code 12600} becomes
 * {@code 12.6K} and {@code 1050000} becomes {@code 1.05M}.
 *
 * <p>Saygınlık Puanı totals reach seven and eight figures in ordinary play, and
 * a raw {@code 12600000} on a sidebar line or a leaderboard entry is a number
 * nobody reads - it is a width the eye has to count digits through. The unit
 * suffixes are deliberately <em>not</em> routed through {@link MessageKey}: K/M/B
 * are numeric unit symbols, not Turkish words, and every other place in this
 * codebase where Java supplies a bare number to a template does exactly the
 * same thing.</p>
 *
 * <p>Precision narrows as the unit grows, because that is where the eye stops
 * caring: thousands keep one decimal ({@code 712.2K}), millions and billions
 * keep two ({@code 1.05M}). A trailing zero is always dropped, so
 * {@code 1200000} reads {@code 1.2M} rather than {@code 1.20M}. Pure, static
 * and dependency-free, the same shape as {@link DurationFormatter}'s
 * arithmetic half.</p>
 */
public final class NumberFormatter {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long BILLION = 1_000_000_000L;

    private NumberFormatter() {
        throw new UnsupportedOperationException("NumberFormatter is a static utility class");
    }

    /**
     * @return {@code value} written with a K/M/B suffix once it is large enough
     *         to need one, and as plain digits below a thousand
     */
    public static String compact(long value) {
        if (value < 0) {
            return "-" + compact(-value);
        }
        if (value < THOUSAND) {
            return Long.toString(value);
        }
        if (value < MILLION) {
            return scaled(value, THOUSAND, 1, "K", MILLION);
        }
        if (value < BILLION) {
            return scaled(value, MILLION, 2, "M", BILLION);
        }
        return scaled(value, BILLION, 2, "B", Long.MAX_VALUE);
    }

    /**
     * Rounding can push a value over its own unit's ceiling ({@code 999_990}
     * rounds to {@code 1000.0K}), which reads as a bigger number than the one
     * above it - so anything that lands on the ceiling is re-rendered in the
     * next unit up instead.
     */
    private static String scaled(long value, long unit, int decimals, String suffix, long ceiling) {
        BigDecimal rounded = BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(unit), decimals, RoundingMode.HALF_UP);
        if (rounded.multiply(BigDecimal.valueOf(unit)).longValue() >= ceiling) {
            return compact(ceiling);
        }
        return rounded.stripTrailingZeros().toPlainString() + suffix;
    }
}
