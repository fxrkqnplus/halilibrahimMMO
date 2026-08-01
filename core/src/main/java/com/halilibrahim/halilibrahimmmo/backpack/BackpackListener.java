package com.halilibrahim.halilibrahimmmo.backpack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

    /**
     * Warms the capacity on join.
     *
     * <p>The contents are deliberately never cached, but the capacity has to
     * be: the talent tree reads it synchronously while rendering (a widening is
     * spent feature points and has to be subtracted from the counter), and the
     * window has to be sized before the contents are even fetched.</p>
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        backpacks.loadUpgrades(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        backpacks.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BackpackHolder holder) {
            backpacks.save(holder.ownerId(), event.getInventory());
        }
    }

    /**
     * Refuses the slots the player has not bought.
     *
     * <p>A backpack window is always a whole number of rows because Minecraft
     * cannot make one that is not, so a 25-slot backpack is drawn as 27 with two
     * blocked panes. Without this, those two panes are ordinary items a player
     * could take out and two slots of free storage they never earned.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) {
            return;
        }
        // Only clicks landing in the backpack itself; the player's own inventory
        // is theirs to rearrange.
        if (event.getClickedInventory() == event.getInventory() && event.getSlot() >= holder.capacity()) {
            event.setCancelled(true);
            return;
        }
        // Shift-clicking from below has no slot of its own to check, so the
        // whole action is refused when nothing in the owned range can take it.
        if (event.isShiftClick() && event.getClickedInventory() != event.getInventory()
                && event.getInventory().firstEmpty() >= holder.capacity()) {
            event.setCancelled(true);
        }
    }

    /** The same rule for a drag, which can spread across several slots at once. */
    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackHolder holder)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && slot >= holder.capacity()) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
