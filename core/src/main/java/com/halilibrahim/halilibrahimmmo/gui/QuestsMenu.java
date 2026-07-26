package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The quest board: one row per quest kind, each listing only what this player
 * is actually working on right now, plus a dedicated bottom row for everything
 * they have already finished.
 *
 * <p>Daily and weekly quests are drawn from a pool and re-rolled every period,
 * so the two middle rows deliberately show the player's own current assignment
 * rather than the whole catalogue - a quest nobody was given is not a quest.
 * Every entry carries its authored description and its XP reward in the
 * tooltip, so hovering answers both "what am I supposed to do" and "what do I
 * get" without opening anything further.</p>
 *
 * <p>A finished quest leaves its own section and moves to the completed row
 * instead of being listed twice, which keeps the active rows an honest to-do
 * list.</p>
 */
public final class QuestsMenu implements GuiView {

    private static final int INFO_SLOT = 4;
    private static final int DAILY_LABEL_SLOT = 9;
    private static final int WEEKLY_LABEL_SLOT = 18;
    private static final int PERMANENT_LABEL_SLOT = 27;
    private static final int COMPLETED_LABEL_SLOT = 36;
    /** Entries fit between a row's label slot and the end of that row. */
    private static final int ENTRIES_PER_ROW = 7;
    private static final int BACK_SLOT = 53;

    private final GuiServices services;

    public QuestsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_QUESTS_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.quests().loadAll(player.getUniqueId()).thenAccept(progressById ->
                player.getScheduler().run(services.plugin(), task -> render(inventory, progressById), null));
    }

    private void render(Inventory inventory, Map<String, QuestProgress> progressById) {
        List<QuestDefinition> assigned = services.quests().definitions().stream()
                .filter(definition -> progressById.containsKey(definition.id()))
                .sorted(Comparator.comparing(QuestDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<QuestDefinition> completed = assigned.stream()
                .filter(definition -> progressById.get(definition.id()).completed())
                .toList();

        inventory.setItem(INFO_SLOT, GuiItems.of(Material.WRITABLE_BOOK,
                services.messages().render(MessageKey.GUI_QUESTS_INFO_ITEM),
                List.of(services.messages().render(MessageKey.GUI_QUESTS_INFO_LORE, Map.of(
                        "tamamlanan", Integer.toString(completed.size()),
                        "toplam", Integer.toString(assigned.size())
                )))));

        renderSection(inventory, DAILY_LABEL_SLOT, Material.CLOCK,
                MessageKey.GUI_QUESTS_SECTION_DAILY, MessageKey.GUI_QUESTS_SECTION_DAILY_LORE,
                activeOfType(assigned, progressById, QuestType.DAILY), progressById);
        renderSection(inventory, WEEKLY_LABEL_SLOT, Material.SUNFLOWER,
                MessageKey.GUI_QUESTS_SECTION_WEEKLY, MessageKey.GUI_QUESTS_SECTION_WEEKLY_LORE,
                activeOfType(assigned, progressById, QuestType.WEEKLY), progressById);
        renderSection(inventory, PERMANENT_LABEL_SLOT, Material.WRITTEN_BOOK,
                MessageKey.GUI_QUESTS_SECTION_PERMANENT, MessageKey.GUI_QUESTS_SECTION_PERMANENT_LORE,
                activeOfType(assigned, progressById, QuestType.PERMANENT), progressById);
        renderSection(inventory, COMPLETED_LABEL_SLOT, Material.LIME_DYE,
                MessageKey.GUI_QUESTS_SECTION_COMPLETED, MessageKey.GUI_QUESTS_SECTION_COMPLETED_LORE,
                completed, progressById);
    }

    private List<QuestDefinition> activeOfType(List<QuestDefinition> assigned, Map<String, QuestProgress> progressById, QuestType type) {
        return assigned.stream()
                .filter(definition -> definition.type() == type)
                .filter(definition -> !progressById.get(definition.id()).completed())
                .toList();
    }

    private void renderSection(
            Inventory inventory, int labelSlot, Material labelIcon, MessageKey labelKey, MessageKey labelLoreKey,
            List<QuestDefinition> definitions, Map<String, QuestProgress> progressById
    ) {
        inventory.setItem(labelSlot, GuiItems.of(labelIcon, services.messages().render(labelKey),
                List.of(services.messages().render(labelLoreKey))));
        if (definitions.isEmpty()) {
            inventory.setItem(labelSlot + 1, GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_QUESTS_SECTION_EMPTY)));
            return;
        }
        int offset = 1;
        for (QuestDefinition definition : definitions) {
            if (offset > ENTRIES_PER_ROW) {
                return;
            }
            inventory.setItem(labelSlot + offset++, questItem(definition, progressById.get(definition.id())));
        }
    }

    private ItemStack questItem(QuestDefinition definition, QuestProgress progress) {
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_DESCRIPTION, Map.of("aciklama", definition.description())));
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE, Map.of(
                "ilerleme", progress.progress() + "/" + definition.target()
        )));
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_REWARD, Map.of(
                "xp", Long.toString(definition.rewardExperience())
        )));
        if (progress.completed()) {
            lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_COMPLETED));
        }
        MessageKey nameKey = progress.completed() ? MessageKey.GUI_QUEST_ITEM_COMPLETED : MessageKey.GUI_QUEST_ITEM;
        return GuiItems.of(definition.icon(), services.messages().render(nameKey, Map.of("gorev", definition.displayName())),
                lore, progress.completed());
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new TasksMenu(services));
        }
    }
}
