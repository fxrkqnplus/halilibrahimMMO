package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.guide.GuideCategory;
import com.halilibrahim.halilibrahimmmo.guide.GuideEntry;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One guide chapter: every page in it, each as an item whose hover text is the
 * page itself.
 *
 * <p>Lore rather than chat is deliberate. A guide read into chat scrolls away
 * and takes the rest of the conversation with it; lore sits still, can be
 * re-read by moving the mouse, and lets a player compare two pages by hovering
 * one then the other. It is also the only surface where a wall of text does not
 * cost anybody else anything. Pages are drawn through
 * {@link GuiItems#ofPreformatted} rather than the wrapping {@link GuiItems#of}:
 * {@code rehber.yml} breaks its own lines, and a second automatic break on top
 * of a deliberate one is what turns a laid-out page into a ragged ladder.</p>
 *
 * <p><strong>Chapters are reachable from inside a chapter.</strong> Reading the
 * handbook end to end used to mean twelve trips back to the front page, one per
 * chapter, and a player looking for a specific answer had to remember which
 * chapter it was in before they could start. The two arrows on the bottom row
 * walk the chapter list in the order the file declares, and the plate between
 * them says where in that list this chapter sits - so the handbook can be read
 * straight through, and browsed by somebody who does not yet know what they are
 * looking for.</p>
 */
public final class GuideCategoryMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int PREVIOUS_SLOT = 47;
    private static final int POSITION_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 51;
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
    /**
     * The chapter list as it was when this screen opened, so the two arrows and
     * the position plate can never disagree with each other - a configuration
     * reload between two clicks would otherwise walk the player somewhere the
     * plate had not predicted.
     */
    private final List<GuideCategory> siblings = new ArrayList<>();

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
                List.of(
                        services.messages().renderText(category.summary()),
                        Component.empty(),
                        services.messages().render(MessageKey.GUI_GUIDE_ENTRY_HINT)
                )
        ));
        List<GuideEntry> entries = category.entries();
        for (int index = 0; index < entries.size() && index < ENTRY_SLOTS.length; index++) {
            GuideEntry entry = entries.get(index);
            List<Component> lore = new ArrayList<>();
            entry.lines().forEach(line -> lore.add(line.isEmpty()
                    ? Component.empty()
                    : services.messages().renderText(line)));
            // ofPreformatted, not of: these lines were broken by whoever wrote
            // rehber.yml and re-wrapping them at 42 characters turned every
            // page into a row-plus-stub ladder. See GuiItems#ofPreformatted.
            inventory.setItem(ENTRY_SLOTS[index],
                    GuiItems.ofPreformatted(entry.icon(), services.messages().renderText(entry.title()), lore));
        }
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        renderNavigation(inventory);
    }

    /**
     * The two arrows and the plate between them.
     *
     * <p>The list wraps, so the last chapter's "next" is the first: a handbook
     * has no end to fall off, and a dead arrow on one screen out of twelve is a
     * button that has to be explained.</p>
     */
    private void renderNavigation(Inventory inventory) {
        siblings.clear();
        siblings.addAll(services.guide().categories());
        int here = indexOfSelf();
        if (siblings.size() < 2 || here < 0) {
            return;
        }
        GuideCategory previous = siblings.get(Math.floorMod(here - 1, siblings.size()));
        GuideCategory next = siblings.get(Math.floorMod(here + 1, siblings.size()));
        inventory.setItem(PREVIOUS_SLOT, GuiItems.of(previous.icon(),
                services.messages().render(MessageKey.GUI_GUIDE_PREVIOUS_CATEGORY),
                List.of(services.messages().renderText(previous.title()))));
        inventory.setItem(NEXT_SLOT, GuiItems.of(next.icon(),
                services.messages().render(MessageKey.GUI_GUIDE_NEXT_CATEGORY),
                List.of(services.messages().renderText(next.title()))));
        inventory.setItem(POSITION_SLOT, GuiItems.of(Material.PAPER,
                services.messages().render(MessageKey.GUI_GUIDE_POSITION, Map.of(
                        "sira", Integer.toString(here + 1),
                        "toplam", Integer.toString(siblings.size())
                )),
                List.of()));
    }

    /** Matched by title, because that is the only thing a chapter is identified by outside the file. */
    private int indexOfSelf() {
        for (int index = 0; index < siblings.size(); index++) {
            if (siblings.get(index).title().equals(category.title())) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new GuideMenu(services, fromMenu));
            return;
        }
        if (siblings.size() < 2) {
            return;
        }
        int here = indexOfSelf();
        if (here < 0) {
            return;
        }
        if (slot == PREVIOUS_SLOT) {
            open(player, siblings.get(Math.floorMod(here - 1, siblings.size())));
            return;
        }
        if (slot == NEXT_SLOT) {
            open(player, siblings.get(Math.floorMod(here + 1, siblings.size())));
        }
    }

    private void open(Player player, GuideCategory target) {
        GuiManager.open(player, new GuideCategoryMenu(services, target, fromMenu));
    }
}
