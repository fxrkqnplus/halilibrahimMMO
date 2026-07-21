package com.halilibrahim.halilibrahimmmo.scoreboard;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

/**
 * Owns each online player's right-hand scoreboard sidebar: a live summary of
 * their name, selected class, global level, XP and prestige.
 *
 * <p>Visibility is a per-player, in-memory-only preference (toggled with
 * {@code /panel}) - it is never persisted to the database, so every fresh
 * join (and therefore every server restart) starts with the sidebar shown,
 * by design.</p>
 */
public interface SidebarService extends PluginService {

    /**
     * Shows the sidebar for a player who just joined (always defaults to
     * visible) and renders its initial content from already-cached data.
     */
    void initialize(Player player);

    /** Clears in-memory state for a player who left the server. */
    void remove(Player player);

    /**
     * Flips the player's visibility preference and immediately shows or hides
     * the sidebar accordingly.
     *
     * @return the new state - {@code true} if the sidebar is now visible.
     */
    boolean toggle(Player player);

    /**
     * Re-renders the sidebar's content from current cached profile/class
     * data. A no-op if the player currently has the sidebar hidden.
     */
    void refresh(Player player);
}
