package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.guide.GuideCategory;
import com.halilibrahim.halilibrahimmmo.guide.GuideEntry;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One guide category: every page in it, each as an item whose hover text is the
 * page itself.
 *
 * <p>Lore rather than chat is deliberate. A guide read into chat scrolls away
 * and takes the rest of the conversation with it; lore sits still, can be
 * re-read by moving the mouse, and lets a player compare two pages by hovering
 * one then the other. It is also the only surface where a wall of text does not
 * cost anybody else anything.</p>
 */
public final class GuideCategoryMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 49;
    /** The interior of a 54-slot board: four rows of seven, edges left to the frame. */
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final GuiServices services;
    private final GuideCategory category;
    private final boolean fromMenu;

    public GuideCategoryMenu(GuiServices services, GuideCategory category, boolean fromMenu) {
        this.services = services;
        this.category = category;
        this.fromMenu = fromMenu;
    }

    @Override
    public Component title() {
        return services.messages().renderText(category.title());
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.BLUE_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(HEADER_SLOT, GuiItems.of(
                category.icon(),
                services.messages().renderText(category.title()),
                List.of(services.messages().renderText(category.summary()))
        ));
        List<GuideEntry> entries = category.entries();
        for (int index = 0; index < entries.size() && index < ENTRY_SLOTS.length; index++) {
            GuideEntry entry = entries.get(index);
            List<Component> lore = new ArrayList<>();
            entry.lines().forEach(line -> lore.add(line.isEmpty()
                    ? Component.empty()
                    : services.messages().renderText(line)));
            inventory.setItem(ENTRY_SLOTS[index],
                    GuiItems.of(entry.icon(), services.messages().renderText(entry.title()), lore));
        }
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new GuideMenu(services, fromMenu));
        }
    }
}
