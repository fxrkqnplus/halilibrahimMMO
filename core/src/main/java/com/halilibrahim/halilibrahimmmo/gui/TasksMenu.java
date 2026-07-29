package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** "Görevler" category sub-hub: quests, collections and the adventure journal. */
public final class TasksMenu implements GuiView {

    private static final int QUESTS_SLOT = 20;
    private static final int COLLECTIONS_SLOT = 22;
    private static final int JOURNAL_SLOT = 24;
    private static final int BACK_SLOT = 40;

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
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.CYAN_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(QUESTS_SLOT, GuiItems.of(Material.WRITTEN_BOOK,
                services.messages().render(MessageKey.GUI_MAIN_QUESTS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_QUESTS_LORE))));
        inventory.setItem(COLLECTIONS_SLOT, GuiItems.of(Material.CHEST,
                services.messages().render(MessageKey.GUI_MAIN_COLLECTIONS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_COLLECTIONS_LORE))));
        inventory.setItem(JOURNAL_SLOT, GuiItems.of(Material.MAP,
                services.messages().render(MessageKey.GUI_MAIN_JOURNAL),
                List.of(services.messages().render(MessageKey.GUI_MAIN_JOURNAL_LORE))));
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
