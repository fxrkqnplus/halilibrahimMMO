package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.journal.JournalEntry;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Read-only grid of the invoking player's most recent quest/collection milestones. */
public final class JournalMenu implements GuiView {

    private static final int RECENT_LIMIT = 53;
    private static final int BACK_SLOT = 53;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault());

    private final GuiServices services;

    public JournalMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_JOURNAL_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        int contentSlots = GuiItems.listBoard(inventory, GuiItems.pane(Material.WHITE_STAINED_GLASS_PANE));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.journal().recent(player.getUniqueId(), RECENT_LIMIT).thenAccept(entries ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (entries.isEmpty()) {
                        inventory.setItem(0, GuiItems.of(Material.MAP, services.messages().render(MessageKey.GUI_JOURNAL_EMPTY_ITEM)));
                        return;
                    }
                    int slot = 0;
                    for (JournalEntry entry : entries) {
                        if (slot >= contentSlots) {
                            break;
                        }
                        String label = entry.referenceName() + " (" + TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(entry.createdAtMillis())) + ")";
                        inventory.setItem(slot++, GuiItems.of(
                                materialFor(entry),
                                services.messages().render(MessageKey.GUI_JOURNAL_ITEM, Map.of("girdi", label)),
                                List.of(services.messages().render(MessageKey.GUI_JOURNAL_ITEM_LORE, Map.of("xp", Long.toString(entry.experienceAwarded()))))
                        ));
                    }
                }, null));
    }

    private Material materialFor(JournalEntry entry) {
        return switch (entry.type()) {
            case QUEST_COMPLETED -> Material.WRITTEN_BOOK;
            case COLLECTION_ENTRY_OBTAINED -> Material.CHEST;
            case COLLECTION_COMPLETED -> Material.ENDER_CHEST;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new TasksMenu(services));
        }
    }
}
