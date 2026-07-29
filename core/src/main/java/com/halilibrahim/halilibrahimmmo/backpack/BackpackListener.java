package com.halilibrahim.halilibrahimmmo.backpack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Writes a backpack back to storage the moment its window closes.
 *
 * <p>Close is the right and the only save point: the server raises it for a
 * window shut by hand, by a kick, by a quit and by a shutdown, so there is no
 * path where a player's changes are left unwritten. Saving on every click would
 * have meant a database write per item moved, for the same end state.</p>
 */
public final class BackpackListener implements Listener {

    private final BackpackService backpacks;

    public BackpackListener(BackpackService backpacks) {
        this.backpacks = backpacks;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackHolder holder) {
            backpacks.save(holder.ownerId(), event.getInventory());
        }
    }
}
