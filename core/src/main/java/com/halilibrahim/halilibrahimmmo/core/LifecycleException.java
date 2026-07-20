package com.halilibrahim.halilibrahimmmo.core;

/**
 * Thrown by a {@link PluginService} when it cannot enable, disable, or reload
 * itself. Thrown from {@code onEnable()} this always results in the plugin
 * disabling itself cleanly rather than continuing in a half-initialized state.
 */
public final class LifecycleException extends RuntimeException {

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
