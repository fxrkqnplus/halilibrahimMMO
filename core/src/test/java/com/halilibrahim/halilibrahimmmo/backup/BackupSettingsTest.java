package com.halilibrahim.halilibrahimmmo.backup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faz 25, madde 20: the backup settings refuse to describe a configuration that
 * would quietly do nothing or quietly fill a disk.
 *
 * <p>Pure, so it runs without a server: every rule here is arithmetic on the
 * record's own components, which is exactly the part a bad {@code yedek.yml}
 * would get wrong.</p>
 */
class BackupSettingsTest {

    private static BackupSettings with(Duration interval, int keep, long maximumBytes) {
        return new BackupSettings(true, "yedek", interval, keep, false, true, true, true, false, maximumBytes);
    }

    @Test
    @DisplayName("A zero interval means on-demand only, not \"every zero minutes\"")
    void zeroIntervalDisablesTheSchedule() {
        assertFalse(with(Duration.ZERO, 4, 1024L).scheduled());
        assertTrue(with(Duration.ofHours(6), 4, 1024L).scheduled());
    }

    @Test
    @DisplayName("A disabled service is never scheduled, whatever the interval says")
    void disabledIsNeverScheduled() {
        BackupSettings off = new BackupSettings(
                false, "yedek", Duration.ofHours(1), 4, false, true, true, true, false, 1024L);
        assertFalse(off.scheduled());
    }

    @Test
    @DisplayName("Retention below one would delete the archive it just wrote")
    void retentionMustKeepSomething() {
        assertThrows(IllegalArgumentException.class, () -> with(Duration.ofHours(1), 0, 1024L));
    }

    @Test
    @DisplayName("A blank directory, a negative interval and a zero size limit are all refused")
    void obviouslyBrokenValuesAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new BackupSettings(true, "  ", Duration.ofHours(1), 4, false, true, true, true, false, 1024L));
        assertThrows(IllegalArgumentException.class, () -> with(Duration.ofHours(-1), 4, 1024L));
        assertThrows(IllegalArgumentException.class, () -> with(Duration.ofHours(1), 4, 0L));
    }

    /**
     * The fallback is what a server boots with when {@code yedek.yml} is
     * unreadable, so it has to be a configuration that actually protects
     * something rather than a set of zeroes.
     */
    @Test
    @DisplayName("The safe defaults protect data and are schedulable")
    void safeDefaultsAreUsable() {
        BackupSettings defaults = BackupSettings.safeDefaults();
        assertTrue(defaults.enabled());
        assertTrue(defaults.scheduled());
        assertTrue(defaults.includePluginData());
        assertTrue(defaults.includePlayerData());
        assertTrue(defaults.onUncleanShutdown());
        // Whole worlds stay off: a fallback must not be the thing that fills the
        // disk of a server whose configuration was already broken.
        assertFalse(defaults.includeWorlds());
        assertEquals(Duration.ofHours(6), defaults.interval());
    }
}
