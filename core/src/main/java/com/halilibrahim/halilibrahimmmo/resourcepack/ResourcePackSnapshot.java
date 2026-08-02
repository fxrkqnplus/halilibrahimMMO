package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Read-only view of one online player's pack state, for the admin screen.
 *
 * <p>Deliberately carries no URL: the screen shows a host and an abbreviated
 * digest, and the full address only ever reaches the console - a lore line is
 * visible to anyone standing behind the admin.</p>
 *
 * @param status             what the plugin has decided so far
 * @param lastReportedStatus the most recent status the client sent, or
 *                           {@code null} if it has not reported anything yet
 */
public record ResourcePackSnapshot(
        ResourcePackStatus status,
        PlayerResourcePackStatusEvent.Status lastReportedStatus
) {
}
