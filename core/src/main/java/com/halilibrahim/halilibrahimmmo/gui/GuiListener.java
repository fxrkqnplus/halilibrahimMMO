package com.halilibrahim.halilibrahimmmo.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Single global click/drag router for every {@link GuiView}. Every click or
 * drag anywhere in the view while one of our inventories is open at the top
 * is cancelled - these menus are display/action screens, never item storage
 * - and only clicks that landed in our own (top) inventory are forwarded to
 * the view.
 */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(event.getView().getTopInventory())) {
            return;
        }
        holder.view().handleClick(player, event.getSlot(), event.getCurrentItem(), event.getClick());
    }

    /**
     * Dragging an item stack across slots (including from the player's own
     * inventory) is a separate event from clicking and was never handled
     * here before - without this, a drag could place a real item into a
     * menu that is supposed to never be usable as storage.
     */
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }
}
