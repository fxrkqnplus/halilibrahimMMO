package com.halilibrahim.halilibrahimmmo.logging;

import org.slf4j.Logger;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Thin wrapper around the SLF4J logger Paper provides through
 * {@code JavaPlugin#getSLF4JLogger()}, adding a debug-mode gate driven by
 * {@code config.yml}/{@code /haliladmin}'s debug toggle.
 *
 * <p>{@link #debug(Supplier)} accepts a lazily-evaluated message so expensive
 * diagnostic string building (formatting collections, joining SQL, ...) never
 * happens when debug mode is off - important for a plugin that needs to stay
 * allocation-light with 500+ players connected.</p>
 */
public final class PluginLogger {

    private final Logger delegate;
    private final BooleanSupplier debugModeSupplier;

    public PluginLogger(Logger delegate, BooleanSupplier debugModeSupplier) {
        this.delegate = delegate;
        this.debugModeSupplier = debugModeSupplier;
    }

    public void info(String message) {
        delegate.info(message);
    }

    public void warn(String message) {
        delegate.warn(message);
    }

    public void warn(String message, Throwable throwable) {
        delegate.warn(message, throwable);
    }

    public void error(String message) {
        delegate.error(message);
    }

    public void error(String message, Throwable throwable) {
        delegate.error(message, throwable);
    }

    public void debug(String message) {
        if (debugModeSupplier.getAsBoolean()) {
            delegate.info("[DEBUG] {}", message);
        }
    }

    public void debug(Supplier<String> messageSupplier) {
        if (debugModeSupplier.getAsBoolean()) {
            delegate.info("[DEBUG] {}", messageSupplier.get());
        }
    }

    public boolean debugModeEnabled() {
        return debugModeSupplier.getAsBoolean();
    }
}
