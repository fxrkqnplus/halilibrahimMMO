package com.halilibrahim.halilibrahimmmo.locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-logic coverage for the unit splitting every player-facing countdown now
 * goes through, in the same spirit as {@code PrestigeRulesTest}/
 * {@code UnlockConditionsTest}. The stub deliberately renders the unit words
 * the way the bundled {@code messages.yml} does, so the assertions read as the
 * exact strings a player sees.
 */
final class DurationFormatterTest {

    private final MessageService messages = new StubMessageService();

    @Test
    void splitsIntoMinutesAndSeconds() {
        assertEquals("2 dakika 10 saniye", DurationFormatter.ofSeconds(messages, 130));
    }

    @Test
    void omitsUnitsThatAreZero() {
        assertEquals("2 saat", DurationFormatter.ofSeconds(messages, 7_200));
        assertEquals("1 gün 1 saniye", DurationFormatter.ofSeconds(messages, 86_401));
    }

    @Test
    void keepsEveryNonZeroUnitLargestFirst() {
        assertEquals("1 gün 2 saat 3 dakika 4 saniye", DurationFormatter.ofSeconds(messages, 93_784));
    }

    @Test
    void reportsAtLeastOneSecondSoAWaitNeverReadsAsZero() {
        assertEquals("1 saniye", DurationFormatter.ofSeconds(messages, 0));
        assertEquals("1 saniye", DurationFormatter.ofMillis(messages, 1));
    }

    @Test
    void roundsPartialSecondsUp() {
        assertEquals("2 saniye", DurationFormatter.ofMillis(messages, 1_001));
    }

    /** Only {@link MessageService#plain} is ever exercised by the formatter. */
    private static final class StubMessageService implements MessageService {

        @Override
        public String plain(MessageKey key, Map<String, String> placeholders) {
            String unit = switch (key) {
                case DURATION_DAY -> "gün";
                case DURATION_HOUR -> "saat";
                case DURATION_MINUTE -> "dakika";
                case DURATION_SECOND -> "saniye";
                default -> throw new IllegalArgumentException("Not a duration key: " + key);
            };
            return placeholders.get("deger") + " " + unit;
        }

        @Override
        public Component render(MessageKey key, Map<String, String> placeholders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Component render(MessageKey key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Component renderText(String template) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(Audience audience, MessageKey key, Map<String, String> placeholders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(Audience audience, MessageKey key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void send(
                Audience audience, MessageKey key, Map<String, String> placeholders, Map<String, Component> components
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void broadcast(MessageKey key, Map<String, String> placeholders) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Component prefixed(Component message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enable() {
        }

        @Override
        public void disable() {
        }
    }
}
