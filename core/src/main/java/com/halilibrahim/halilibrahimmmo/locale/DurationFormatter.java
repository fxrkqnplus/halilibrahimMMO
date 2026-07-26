package com.halilibrahim.halilibrahimmmo.locale;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw millisecond/second duration into the human-readable Turkish
 * phrase every player-facing countdown, cooldown and remaining-time value is
 * shown as ("2 dakika 10 saniye" rather than "130 saniye").
 *
 * <p>The unit words themselves are <em>not</em> written in Java: each one is
 * its own {@link MessageKey} template in {@code messages.yml}, rendered to bare
 * text through {@link MessageService#plain}. That keeps this class inside the
 * project's "no Turkish literal ever built in Java" rule while still producing
 * a plain {@code String} - which is what the surrounding templates need, since
 * they substitute it as an unparsed placeholder value rather than a nested
 * component.</p>
 *
 * <p>Only non-zero units appear, largest first, so 7 days reads "7 gün" and not
 * "7 gün 0 saat 0 dakika 0 saniye". A duration that rounds down to nothing
 * still reports one second rather than an empty string, because every call site
 * is a "you must wait" or "this much is left" message where zero would read as
 * a bug.</p>
 */
public final class DurationFormatter {

    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long SECONDS_PER_HOUR = 3_600L;
    private static final long SECONDS_PER_DAY = 86_400L;

    private DurationFormatter() {
        throw new UnsupportedOperationException("DurationFormatter is a static utility class");
    }

    /** Rounds up, so a 500 ms remainder is still shown as a full second rather than disappearing. */
    public static String ofMillis(MessageService messages, long millis) {
        return ofSeconds(messages, Math.max(0L, millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND);
    }

    public static String ofSeconds(MessageService messages, long totalSeconds) {
        long remaining = Math.max(0L, totalSeconds);
        List<String> parts = new ArrayList<>(4);
        remaining = appendUnit(messages, parts, MessageKey.DURATION_DAY, remaining, SECONDS_PER_DAY);
        remaining = appendUnit(messages, parts, MessageKey.DURATION_HOUR, remaining, SECONDS_PER_HOUR);
        remaining = appendUnit(messages, parts, MessageKey.DURATION_MINUTE, remaining, SECONDS_PER_MINUTE);
        appendUnit(messages, parts, MessageKey.DURATION_SECOND, remaining, 1L);
        if (parts.isEmpty()) {
            parts.add(messages.plain(MessageKey.DURATION_SECOND, Map.of("deger", "1")));
        }
        return String.join(" ", parts);
    }

    private static long appendUnit(MessageService messages, List<String> parts, MessageKey key, long seconds, long unitSeconds) {
        long value = seconds / unitSeconds;
        if (value > 0) {
            parts.add(messages.plain(key, Map.of("deger", Long.toString(value))));
        }
        return seconds % unitSeconds;
    }
}
