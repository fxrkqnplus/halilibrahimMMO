package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    /** Same as {@link #of(Material, Component, List)}, with a purely cosmetic enchant glint when {@code glowing}. */
    public static ItemStack of(Material material, Component name, List<Component> lore, boolean glowing) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(undecorate(name));
        meta.lore(lore.stream().map(GuiItems::undecorate).toList());
        meta.setEnchantmentGlintOverride(glowing);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack backButton(MessageService messages) {
        return of(Material.ARROW, messages.render(MessageKey.GUI_BACK));
    }

    public static ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private static Component undecorate(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
