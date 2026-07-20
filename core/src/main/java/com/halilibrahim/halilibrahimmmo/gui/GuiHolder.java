package com.halilibrahim.halilibrahimmmo.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Marks an {@link Inventory} as ours so {@link GuiListener} can route its clicks back to the owning {@link GuiView}. */
public final class GuiHolder implements InventoryHolder {

    private final GuiView view;
    private Inventory inventory;

    public GuiHolder(GuiView view) {
        this.view = view;
    }

    public GuiView view() {
        return view;
    }

    void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
