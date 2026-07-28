package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.trade.TradeScreenRenderer;
import com.halilibrahim.halilibrahimmmo.trade.TradeSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * The GUI-side half of {@link TradeScreenRenderer}: lets the trade service open,
 * repaint and close trade screens without importing the {@code gui} package
 * that already imports it.
 *
 * <p>Repainting deliberately writes into the inventory the player already has
 * open instead of reopening the screen. Reopening would fire a close event,
 * which the trade screen treats as walking away from the table - so every
 * single offered item would cancel the trade that placed it.</p>
 */
public final class TradeScreenBridge implements TradeScreenRenderer {

    private final GuiServices services;

    public TradeScreenBridge(GuiServices services) {
        this.services = services;
    }

    @Override
    public void open(Player player, TradeSession session) {
        GuiManager.open(player, new TradeMenu(services, player.getUniqueId()));
    }

    @Override
    public void refresh(TradeSession session) {
        repaint(session.firstPlayerId());
        repaint(session.secondPlayerId());
    }

    private void repaint(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Inventory open = player.getOpenInventory().getTopInventory();
        if (open.getHolder() instanceof GuiHolder holder && holder.view() instanceof TradeMenu menu) {
            menu.render(open);
        }
    }

    @Override
    public void close(Player player) {
        Inventory open = player.getOpenInventory().getTopInventory();
        if (open.getHolder() instanceof GuiHolder holder && holder.view() instanceof TradeMenu) {
            player.closeInventory();
        }
    }
}
