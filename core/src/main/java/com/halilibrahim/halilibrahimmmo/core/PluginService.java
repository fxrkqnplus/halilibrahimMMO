package com.halilibrahim.halilibrahimmmo.core;

/**
 * Lifecycle contract implemented by every internal subsystem (config, locale,
 * database, ...). The main plugin class keeps an ordered list of every registered
 * {@link PluginService} and calls {@link #enable()} in registration order during
 * {@code onEnable()}, then {@link #disable()} in reverse order during
 * {@code onDisable()}, guaranteeing dependents are always shut down before their
 * dependencies.
 */
public interface PluginService {

    /**
     * Performs first-time initialization. Called exactly once, in dependency order,
     * during plugin startup. Implementations should throw {@link LifecycleException}
     * on unrecoverable failure so the plugin can abort startup cleanly.
     */
    void enable();

    /**
     * Releases every resource acquired in {@link #enable()} (thread pools, file
     * handles, connections, ...). Called exactly once, in reverse dependency order,
     * during plugin shutdown. Must never throw for a service that was successfully
     * enabled.
     */
    void disable();

    /**
     * Re-reads configuration and re-initializes internal state without a full
     * {@link #disable()}/{@link #enable()} cycle. The default implementation is a
     * no-op for services that have nothing to reload.
     */
    default void reload() {
    }
}
