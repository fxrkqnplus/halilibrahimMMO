package com.halilibrahim.halilibrahimmmo.backup;

import com.halilibrahim.halilibrahimmmo.core.PluginService;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps copies of everything a crash could take: the plugin's own configuration
 * and database, and the player data the server writes.
 *
 * <p><strong>Why the plugin owns this at all.</strong> Backing a server up is
 * normally the host's job, and on a well-run host it still is - but the failure
 * this exists for is the one that leaves no trace in a host's nightly snapshot:
 * a power cut between two autosaves, with the SQLite database half-written and
 * a player's inventory a day out of date. The archive this writes is taken
 * after a flush, with the database snapshotted through SQLite's own consistent
 * copy, so what comes out of it is a state the server was actually in.</p>
 *
 * <p>Every method is asynchronous and none of them touch Bukkit API off the main
 * thread; the one thing that must happen on it - asking the worlds to flush -
 * is hopped there explicitly by the implementation.</p>
 */
public interface BackupService extends PluginService {

    BackupSettings settings();

    /**
     * Takes one backup now.
     *
     * <p>Refuses rather than queues when one is already running: two archives of
     * the same moment are not twice as safe, and a queue is how a struggling
     * server turns one slow backup into ten.</p>
     *
     * @param label a short, filename-safe word describing why - {@code acilis},
     *              {@code cokme}, {@code zamanli}, {@code elle}
     */
    CompletableFuture<BackupResult> backupNow(String label);

    /** When the last successful backup finished, if there has been one this session. */
    Optional<Instant> lastBackupAt();

    /**
     * Whether the previous run of the server ended without {@code onDisable}
     * being called - a crash, a {@code kill -9}, a power cut.
     *
     * <p>Answered from a marker file written on enable and deleted on disable,
     * which is the only signal a plugin gets at all: there is no "was the last
     * shutdown clean" flag anywhere in the platform.</p>
     */
    boolean previousRunCrashed();
}
