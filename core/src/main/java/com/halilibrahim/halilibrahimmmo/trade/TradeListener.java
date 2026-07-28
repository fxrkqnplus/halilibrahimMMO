package com.halilibrahim.halilibrahimmmo.trade;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Makes sure nobody's goods are left sitting in a session nobody can reach.
 *
 * <p>Cancelling on quit rather than trying to preserve the trade is the safe
 * direction: the quitting player is still a live {@code Player} at this point,
 * so their items go straight back into an inventory that is about to be saved,
 * and their partner gets theirs back with an explanation.</p>
 */
public final class TradeListener implements Listener {

    private final TradeService trades;

    public TradeListener(TradeService trades) {
        this.trades = trades;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        trades.cancel(player);
        trades.evict(player.getUniqueId());
    }
}
