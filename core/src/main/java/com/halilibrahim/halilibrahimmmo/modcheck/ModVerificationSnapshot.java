package com.halilibrahim.halilibrahimmmo.modcheck;

/**
 * Read-only view of one online player's verification state, for the admin
 * screen. Deliberately carries no mod list: the admin screen shows counts, and
 * the full list only ever reaches the console, and only in debug mode.
 */
public record ModVerificationSnapshot(
        ModVerificationStatus status,
        String clientBrand,
        int reportedModCount
) {
}
