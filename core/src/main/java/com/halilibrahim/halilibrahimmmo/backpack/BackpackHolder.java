package com.halilibrahim.halilibrahimmmo.backpack;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Marks an inventory as somebody's backpack.
 *
 * <p>Deliberately <em>not</em> a {@code GuiHolder}. Every inventory this plugin
 * opens through {@code GuiManager} is a display screen whose clicks are
 * unconditionally cancelled - an invariant this codebase has held since the GUI
 * framework was written, and which the drag handler was later added to close a
 * hole in. The backpack is the exact opposite: it is real storage and its
 * clicks must go through untouched. Giving it its own holder type is what keeps
 * both statements true at once, rather than teaching the shared listener an
 * exception it could later get wrong.</p>
 */
public final class BackpackHolder implements InventoryHolder {

    private final UUID ownerId;
    private Inventory inventory;

    public BackpackHolder(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
