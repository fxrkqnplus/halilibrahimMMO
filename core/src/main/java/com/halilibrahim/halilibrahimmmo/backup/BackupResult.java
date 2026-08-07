package com.halilibrahim.halilibrahimmmo.backup;

/**
 * What one backup attempt did.
 *
 * <p>A value rather than an exception, because a failed backup is a normal
 * outcome an operator has to be told about in Turkish - not a stack trace. The
 * {@code reason} is a {@link Reason} the caller maps to a message rather than a
 * sentence built here, keeping every player- and admin-facing string in
 * {@code messages.yml}.</p>
 *
 * @param fileName the archive that was written, empty when none was
 * @param bytes    how large it is
 * @param entries  how many files went into it
 */
public record BackupResult(Reason reason, String fileName, long bytes, int entries) {

    public enum Reason {
        /** Written and verified. */
        OK,
        /** Backups are switched off in {@code yedek.yml}. */
        DISABLED,
        /** One is already running; a second was refused rather than queued. */
        ALREADY_RUNNING,
        /** Nothing was selected to include, so there was nothing to write. */
        NOTHING_TO_DO,
        /** The archive grew past {@code maksimum-boyut-mb} and was deleted again. */
        TOO_LARGE,
        /** The destination could not be written to - permissions, a full disk, a read-only mount. */
        WRITE_FAILED
    }

    public boolean successful() {
        return reason == Reason.OK;
    }

    public static BackupResult of(Reason reason) {
        return new BackupResult(reason, "", 0L, 0);
    }
}
