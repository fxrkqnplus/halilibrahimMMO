package com.halilibrahim.halilibrahimmmo.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Opens a {@link GuiView} for a player, binding it to a fresh {@link GuiHolder}-owned inventory. */
public final class GuiManager {

    private GuiManager() {
        throw new UnsupportedOperationException("GuiManager is a static utility class");
    }

    public static void open(Player player, GuiView view) {
        GuiHolder holder = new GuiHolder(view);
        Inventory inventory = Bukkit.createInventory(holder, view.size(), view.title());
        holder.bind(inventory);
        player.openInventory(inventory);
        view.populate(player, inventory);
    }
}
