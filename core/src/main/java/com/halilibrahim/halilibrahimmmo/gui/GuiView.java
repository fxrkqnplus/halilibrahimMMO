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

    /**
     * Called for a click in the player's <em>own</em> inventory while this view
     * is open. Still pre-cancelled, like every other click in one of our
     * screens - a view that reacts to this is expected to move the item itself
     * rather than let Bukkit do it.
     *
     * <p>Only the trade screen overrides this, and only so a player can put
     * something on the table by clicking it where it already is. Every other
     * menu ignores its own inventory entirely.</p>
     */
    default void handleOwnInventoryClick(Player player, int slot, ItemStack clicked, ClickType click) {
    }

    /**
     * Called when this view's inventory closes for any reason - the player
     * pressing escape, another menu being opened over it, or the server closing
     * it. A view that holds state outside the inventory has to release it here.
     */
    default void handleClose(Player player) {
    }
}
