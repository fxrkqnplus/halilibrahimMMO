package com.halilibrahim.halilibrahimmmo.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * One inventory-backed menu screen. Implementations are stateless with respect
 * to Bukkit objects - all per-open state lives in the {@link Inventory} that
 * {@link GuiManager#open(Player, GuiView)} creates and binds to a
 * {@link GuiHolder}, so a single {@code GuiView} instance is only ever shown to
 * the one player it was constructed for.
 */
public interface GuiView {

    Component title();

    int size();

    /**
     * Fills {@code inventory} with items. May resolve asynchronously (loading
     * player data through a service) as long as {@code inventory} is only
     * mutated after hopping back to {@code player}'s own region scheduler - the
     * same rule every other Bukkit-API-touching callback in this plugin follows.
     */
    void populate(Player player, Inventory inventory);

    /** Called for every click inside this view's inventory; clicks are always pre-cancelled. */
    void handleClick(Player player, int slot, ItemStack clicked, ClickType click);
}
