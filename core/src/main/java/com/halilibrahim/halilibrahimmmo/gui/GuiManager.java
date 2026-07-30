package com.halilibrahim.halilibrahimmmo.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Opens a {@link GuiView} for a player, binding it to a fresh {@link GuiHolder}-owned inventory. */
public final class GuiManager {

    /**
     * Something that may open a different screen than the one asked for.
     *
     * <p>Exists for exactly one case: a player who has prestiged owes the game
     * a choice of permanent reward, and every MMO screen has to lead them back
     * to it until they make it. Enforcing that at each of the twenty-odd
     * commands and the dozens of menu-to-menu links would have meant twenty-odd
     * places to forget it; there is one door into every screen in this plugin
     * and this is it.</p>
     */
    @FunctionalInterface
    public interface OpenGuard {

        /**
         * @return {@code true} when this guard has handled the request itself
         *         and {@code requested} must not be shown
         */
        boolean redirect(Player player, GuiView requested);
    }

    private static final OpenGuard NONE = (player, requested) -> false;

    /**
     * Bound once, from the plugin bootstrap, after the services the guard needs
     * exist - the same forward-reference shape {@code TradeScreenBridge} uses to
     * hand the trade service its renderer.
     */
    private static volatile OpenGuard guard = NONE;

    private GuiManager() {
        throw new UnsupportedOperationException("GuiManager is a static utility class");
    }

    public static void bindGuard(OpenGuard newGuard) {
        guard = newGuard == null ? NONE : newGuard;
    }

    public static void open(Player player, GuiView view) {
        if (guard.redirect(player, view)) {
            return;
        }
        GuiHolder holder = new GuiHolder(view);
        Inventory inventory = Bukkit.createInventory(holder, view.size(), view.title());
        holder.bind(inventory);
        player.openInventory(inventory);
        view.populate(player, inventory);
    }
}
