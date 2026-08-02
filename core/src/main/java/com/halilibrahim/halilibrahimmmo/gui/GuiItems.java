package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/** Builds display-only {@link ItemStack}s from already-rendered {@link Component}s. */
public final class GuiItems {

    private GuiItems() {
        throw new UnsupportedOperationException("GuiItems is a static utility class");
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        return of(material, name, lore, false);
    }

    public static ItemStack of(Material material, Component name) {
        return of(material, name, List.of());
    }

    /**
     * Overloads that only add a pack model, so a call site can gain one by
     * appending a single argument rather than padding out to the widest form.
     * A definition without a {@code model:} line passes {@code null} here and
     * the item is built exactly as before.
     */
    public static ItemStack of(Material material, Component name, NamespacedKey model) {
        return of(material, name, List.of(), false, model);
    }

    public static ItemStack of(Material material, Component name, List<Component> lore, NamespacedKey model) {
        return of(material, name, lore, false, model);
    }

    /**
     * Same as {@link #of(Material, Component, List)}, with a purely cosmetic
     * enchant glint when {@code glowing}.
     *
     * <p>Every lore line goes through {@link LoreWrapper} on the way in. That is
     * done here, once, rather than at the call sites: Minecraft never wraps
     * lore, so any description long enough to be worth writing used to run off
     * the right-hand edge of the screen as a single thin ribbon, and every menu
     * in the plugin builds its tooltips through this one method.</p>
     */
    public static ItemStack of(Material material, Component name, List<Component> lore, boolean glowing) {
        return of(material, name, lore, glowing, null);
    }

    /**
     * Same again, additionally pointing the item at a texture the server's
     * resource pack supplies.
     *
     * <p>{@code model} names an item model definition the pack ships at
     * {@code assets/hmmo/items/&lt;path&gt;.json}. It is always optional: a
     * {@code null} key - which is what every entry without a {@code model:} line
     * in its YAML produces - leaves the item exactly as it was, so a server that
     * never regenerates its config files and a player whose pack failed to apply
     * both keep seeing the plain {@link Material}. That fallback is the whole
     * reason the material is still chosen per entry rather than replaced by a
     * single placeholder.</p>
     *
     * <p>Uses the {@code item_model} data component rather than
     * {@code setCustomModelData}, which has been deprecated since 1.21.5.</p>
     */
    public static ItemStack of(Material material, Component name, List<Component> lore,
                               boolean glowing, NamespacedKey model) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(undecorate(name));
        meta.lore(LoreWrapper.wrapAll(lore).stream().map(GuiItems::undecorate).toList());
        meta.setEnchantmentGlintOverride(glowing);
        if (model != null) {
            meta.setItemModel(model);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** A {@link Material#PLAYER_HEAD} actually wearing {@code owner}'s skin, not a generic Steve head. */
    public static ItemStack playerHead(OfflinePlayer owner, Component name, List<Component> lore) {
        ItemStack item = of(Material.PLAYER_HEAD, name, lore);
        if (item.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return item;
    }

    public static ItemStack backButton(MessageService messages) {
        return of(Material.ARROW, messages.render(MessageKey.GUI_BACK));
    }

    /** Darker filler used for a menu's outer frame, so the interactive middle stands out. */
    public static ItemStack border() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * A nameless pane of any material, for screens that frame themselves in a
     * colour of their own - a class's own screens use the class's
     * {@code cerceve}, so the player's class is legible before a single label
     * is read.
     */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Lays a footer row across the bottom of a list-shaped screen and answers
     * how many slots are left above it for entries.
     *
     * <p>A list cannot be framed on all four sides the way a hub can - its
     * entries need whole rows - but it can still be closed off at the bottom,
     * which is what stops the back button from being one more anonymous square
     * at the end of the list and what gives the screen an edge. Returning the
     * usable count is the point: every one of these lists used to write from
     * slot zero with nothing bounding it, so a long enough list would silently
     * overwrite its own back button.</p>
     *
     * @return the exclusive upper bound on entry slots
     */
    public static int listBoard(Inventory inventory, ItemStack footer) {
        int contentSlots = inventory.getSize() - 9;
        for (int slot = contentSlots; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, footer);
        }
        return contentSlots;
    }

    /** Lays {@code frame} around the outer edge of an inventory and {@code fill} through the middle. */
    public static void frame(Inventory inventory, ItemStack frame, ItemStack fill) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean edge = slot < 9 || slot >= inventory.getSize() - 9 || slot % 9 == 0 || slot % 9 == 8;
            inventory.setItem(slot, edge ? frame : fill);
        }
    }

    private static Component undecorate(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
