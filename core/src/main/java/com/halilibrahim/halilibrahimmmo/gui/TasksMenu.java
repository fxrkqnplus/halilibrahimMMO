package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** "Görevler" category sub-hub: quests, collections and the adventure journal. */
public final class TasksMenu implements GuiView {

    private static final int QUESTS_SLOT = 10;
    private static final int COLLECTIONS_SLOT = 12;
    private static final int JOURNAL_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;

    public TasksMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_TASKS_TITLE);
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
        inventory.setItem(QUESTS_SLOT, GuiItems.of(Material.WRITTEN_BOOK, services.messages().render(MessageKey.GUI_MAIN_QUESTS)));
        inventory.setItem(COLLECTIONS_SLOT, GuiItems.of(Material.CHEST, services.messages().render(MessageKey.GUI_MAIN_COLLECTIONS)));
        inventory.setItem(JOURNAL_SLOT, GuiItems.of(Material.MAP, services.messages().render(MessageKey.GUI_MAIN_JOURNAL)));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case QUESTS_SLOT -> GuiManager.open(player, new QuestsMenu(services));
            case COLLECTIONS_SLOT -> GuiManager.open(player, new CollectionsMenu(services));
            case JOURNAL_SLOT -> GuiManager.open(player, new JournalMenu(services));
            case BACK_SLOT -> GuiManager.open(player, new AnaMenu(services));
            default -> {
            }
        }
    }
}
