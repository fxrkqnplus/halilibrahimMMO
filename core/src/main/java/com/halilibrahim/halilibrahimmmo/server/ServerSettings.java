package com.halilibrahim.halilibrahimmmo.server;

/**
 * Immutable snapshot of {@code sunucu.yml}. Deliberately carries behaviour
 * switches only - every player-visible string this package renders (the TAB
 * header/footer, the sleep announcement, the live-map link) is a
 * {@code MessageKey} in {@code messages.yml}, so nothing here is text.
 *
 * @param singleSleeperSkipsNight one player finishing a night's sleep brings morning for the whole world
 * @param sleepClearsWeather      the same moment also stops rain/thunder
 * @param announceSleep           the world's players are told who skipped the night
 * @param tabListEnabled          the plugin owns the TAB list header/footer and entry names
 * @param tabRefreshTicks         how often the TAB list is repainted, already converted to ticks
 */
public record ServerSettings(
        boolean singleSleeperSkipsNight,
        boolean sleepClearsWeather,
        boolean announceSleep,
        boolean tabListEnabled,
        long tabRefreshTicks
) {
}
