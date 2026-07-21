package com.halilibrahim.halilibrahimmmo.config;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

/**
 * Owns {@code config.yml}: copying the bundled Turkish-commented default to the
 * plugin data folder on first run, parsing it into an immutable {@link CoreConfig},
 * and re-parsing it on demand through {@link #reload()}.
 */
public interface ConfigService extends PluginService {

    /**
     * @return the current, immutable configuration snapshot. Safe to call from any thread.
     */
    CoreConfig coreConfig();

    /**
     * Applies a runtime-only debug mode override (see {@code /haliladmin}'s debug toggle),
     * without touching {@code config.yml} on disk. The override is cleared the next
     * time {@link #reload()} re-reads the file from disk.
     */
    void setDebugModeOverride(boolean enabled);
}
