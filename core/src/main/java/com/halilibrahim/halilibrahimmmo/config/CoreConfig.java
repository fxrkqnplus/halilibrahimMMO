package com.halilibrahim.halilibrahimmmo.config;

/**
 * Immutable snapshot of {@code config.yml} at a point in time. {@link ConfigService}
 * hands out a fresh instance every time the file is (re)loaded, and swaps its
 * internal reference atomically so concurrent readers never observe a partially
 * updated configuration.
 *
 * @param locale           the ISO-ish locale code the server operates under, e.g. {@code "tr"}
 * @param debugMode        whether verbose debug logging is currently active - may be a runtime
 *                         override applied through {@code /rpg hataayiklama} rather than the value
 *                         literally on disk, see {@link #withDebugMode(boolean)}
 * @param databaseSettings resolved database connection settings
 */
public record CoreConfig(String locale, boolean debugMode, DatabaseSettings databaseSettings) {

    /**
     * @return a copy of this configuration with only {@code debugMode} changed. Used by
     * {@code /rpg hataayiklama} to flip debug logging for the current session without
     * requiring a full config reload.
     */
    public CoreConfig withDebugMode(boolean newDebugMode) {
        return new CoreConfig(locale, newDebugMode, databaseSettings);
    }
}
