package com.halilibrahim.halilibrahimmmo.trade;

import org.bukkit.entity.Player;

/**
 * How {@link TradeService} shows a trade to the two players in it, without
 * depending on the GUI layer that depends on it.
 *
 * <p>The same forward-reference shape {@code SkillExperienceListener} uses to
 * let {@code skills} notify {@code classes} without importing it: the interface
 * is declared here, the implementation lives in {@code gui}, and the plugin
 * bootstrap connects the two once both exist.</p>
 */
public interface TradeScreenRenderer {

    /** Opens a fresh trade screen for one side of {@code session}. */
    void open(Player player, TradeSession session);

    /** Re-renders whichever of the two screens are currently open, after the table changed. */
    void refresh(TradeSession session);

    /**
     * Closes a player's trade screen without that close being treated as a
     * cancellation - used when the trade has already ended for another reason.
     */
    void close(Player player);
}
