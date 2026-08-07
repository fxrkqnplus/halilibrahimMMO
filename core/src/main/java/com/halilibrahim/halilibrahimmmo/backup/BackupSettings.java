package com.halilibrahim.halilibrahimmmo.backup;

import java.time.Duration;

/**
 * The whole of {@code yedek.yml}, as an immutable snapshot.
 *
 * <p>A record for the same reason {@code CoreConfig} is one: every reload
 * produces a brand new instance published atomically, so a backup that is
 * already running can never see half of an edit.</p>
 *
 * @param enabled            master switch. Off means nothing is written and
 *                           nothing is scheduled - not even the crash check.
 * @param directory          where archives go, relative to the server root
 * @param interval           how often a scheduled backup runs
 * @param keep               how many archives to keep; older ones are deleted
 *                           newest-first-survives
 * @param onStartup          take one on every startup, not only after a crash
 * @param onUncleanShutdown  take one when the previous run did not shut down
 *                           cleanly - the case this whole subsystem exists for
 * @param includePluginData  the plugin's own folder: every YAML and the SQLite
 *                           database
 * @param includePlayerData  every world's {@code playerdata} and {@code stats},
 *                           which is where a player's inventory actually lives
 * @param includeWorlds      the whole world folders. Off by default: on a
 *                           long-lived survival map this is gigabytes, and an
 *                           admin should choose to pay that rather than discover
 *                           it.
 * @param maximumArchiveBytes refuse to write an archive larger than this rather
 *                           than filling the disk the server is running on
 */
public record BackupSettings(
        boolean enabled,
        String directory,
        Duration interval,
        int keep,
        boolean onStartup,
        boolean onUncleanShutdown,
        boolean includePluginData,
        boolean includePlayerData,
        boolean includeWorlds,
        long maximumArchiveBytes
) {

    /** What the plugin falls back to when {@code yedek.yml} cannot be read at all. */
    public static BackupSettings safeDefaults() {
        return new BackupSettings(
                true, "hmmo-yedekler", Duration.ofHours(6), 8,
                false, true, true, true, false, 2L * 1024L * 1024L * 1024L
        );
    }

    public BackupSettings {
        if (directory == null || directory.isBlank()) {
            throw new IllegalArgumentException("Backup directory must not be blank.");
        }
        if (interval == null || interval.isNegative()) {
            throw new IllegalArgumentException("Backup interval must not be negative.");
        }
        if (keep < 1) {
            throw new IllegalArgumentException("Backup retention must keep at least one archive.");
        }
        if (maximumArchiveBytes < 1L) {
            throw new IllegalArgumentException("Maximum archive size must be positive.");
        }
    }

    /** {@code true} when the interval is zero, meaning "only on demand and on the events below". */
    public boolean scheduled() {
        return enabled && !interval.isZero();
    }
}
