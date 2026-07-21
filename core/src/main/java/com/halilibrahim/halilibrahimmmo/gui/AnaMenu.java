package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Central hub, reorganized into four categories rather than eleven flat
 * icons: İlerleme ({@link ProgressionMenu}), Kabiliyetlerim
 * ({@link AbilitiesMenu}, kept top-level as this codebase's most involved
 * system), Görevler ({@link TasksMenu}) and Topluluk
 * ({@link CommunityMenu}).
 */
public final class AnaMenu implements GuiView {

    private static final int PROGRESSION_SLOT = 11;
    private static final int ABILITIES_SLOT = 13;
    private static final int TASKS_SLOT = 15;
    private static final int COMMUNITY_SLOT = 17;

    private final GuiServices services;

    public AnaMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_MAIN_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        ItemStack filler = GuiItems.filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(PROGRESSION_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE, services.messages().render(MessageKey.GUI_MAIN_CATEGORY_PROGRESSION)));
        inventory.setItem(ABILITIES_SLOT, GuiItems.of(Material.BLAZE_POWDER, services.messages().render(MessageKey.GUI_MAIN_ABILITIES)));
        inventory.setItem(TASKS_SLOT, GuiItems.of(Material.WRITTEN_BOOK, services.messages().render(MessageKey.GUI_MAIN_CATEGORY_TASKS)));
        inventory.setItem(COMMUNITY_SLOT, GuiItems.of(Material.BEACON, services.messages().render(MessageKey.GUI_MAIN_CATEGORY_COMMUNITY)));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case PROGRESSION_SLOT -> GuiManager.open(player, new ProgressionMenu(services));
            case ABILITIES_SLOT -> GuiManager.open(player, new AbilitiesMenu(services));
            case TASKS_SLOT -> GuiManager.open(player, new TasksMenu(services));
            case COMMUNITY_SLOT -> GuiManager.open(player, new CommunityMenu(services));
            default -> {
            }
        }
    }
}
