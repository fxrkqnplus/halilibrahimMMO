package com.halilibrahim.halilibrahimmmo.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.function.IntFunction;

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

    /** Draws nothing until bound, so an unbound server behaves exactly as before. */
    private static final IntFunction<Component> NO_BACKGROUND = size -> Component.empty();

    /**
     * Prepends a resource-pack background to every screen's title.
     *
     * <p>Bound in the same shape as {@link #bindGuard}, and for the same reason:
     * there are 35 {@code GuiView}s and prepending a background at each one's
     * {@code title()} would be 35 places to forget it. The technique itself is
     * why it belongs here rather than in {@code messages.yml} - the background
     * is chosen by the screen's <em>size</em>, which only this method knows.</p>
     */
    private static volatile IntFunction<Component> background = NO_BACKGROUND;

    private GuiManager() {
        throw new UnsupportedOperationException("GuiManager is a static utility class");
    }

    public static void bindGuard(OpenGuard newGuard) {
        guard = newGuard == null ? NONE : newGuard;
    }

    public static void bindBackgrounds(IntFunction<Component> newBackground) {
        background = newBackground == null ? NO_BACKGROUND : newBackground;
    }

    /** Restores the default so a reload cannot inherit a stale renderer. */
    public static void unbindBackgrounds() {
        background = NO_BACKGROUND;
    }

    public static void open(Player player, GuiView view) {
        if (guard.redirect(player, view)) {
            return;
        }
        GuiHolder holder = new GuiHolder(view);
        Component title = background.apply(view.size()).append(view.title());
        Inventory inventory = Bukkit.createInventory(holder, view.size(), title);
        holder.bind(inventory);
        player.openInventory(inventory);
        view.populate(player, inventory);
    }
}
