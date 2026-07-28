package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.quests.QuestDefinition;
import com.halilibrahim.halilibrahimmmo.quests.QuestProgress;
import com.halilibrahim.halilibrahimmmo.quests.QuestService;
import com.halilibrahim.halilibrahimmmo.quests.QuestType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The quest board: a column of section tabs on the left, and the selected
 * section's quests filling everything to the right of it.
 *
 * <p>The previous board put all four sections on screen at once, one row each,
 * which meant a section could show seven quests before silently dropping the
 * rest and every row competed with the other three for attention. Showing one
 * section at a time gives each of them 42 slots instead of 7 - enough for the
 * discovery quests added in this phase and anything a server adds later - and
 * leaves the eye with one list to read instead of four.</p>
 *
 * <p>Daily and weekly quests are drawn from a pool and re-rolled every period,
 * so their tabs deliberately show the player's own current assignment rather
 * than the whole catalogue - a quest nobody was given is not a quest. A
 * finished quest leaves its own section for the completed tab instead of being
 * listed twice, which keeps the active tabs an honest to-do list.</p>
 */
public final class QuestsMenu implements GuiView {

    /** The fourth tab is not a {@link QuestType} - it cuts across all three. */
    public enum Section {
        DAILY, WEEKLY, PERMANENT, COMPLETED
    }

    private static final int[] TAB_SLOTS = {0, 9, 18, 27};
    private static final Section[] TABS = {Section.DAILY, Section.WEEKLY, Section.PERMANENT, Section.COMPLETED};
    private static final int INFO_SLOT = 36;
    private static final int BACK_SLOT = 45;
    private static final int[] DIVIDER_SLOTS = {1, 10, 19, 28, 37, 46};
    private static final int[] ENTRY_SLOTS = {
            2, 3, 4, 5, 6, 7, 8,
            11, 12, 13, 14, 15, 16, 17,
            20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 34, 35,
            38, 39, 40, 41, 42, 43, 44,
            47, 48, 49, 50, 51, 52, 53
    };

    private final GuiServices services;
    private final Section section;
    private final Map<Integer, String> slotToQuestId = new LinkedHashMap<>();

    public QuestsMenu(GuiServices services) {
        this(services, Section.DAILY);
    }

    public QuestsMenu(GuiServices services, Section section) {
        this.services = services;
        this.section = section;
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
        ItemStack divider = GuiItems.border();
        for (int slot : DIVIDER_SLOTS) {
            inventory.setItem(slot, divider);
        }
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        for (int index = 0; index < TABS.length; index++) {
            inventory.setItem(TAB_SLOTS[index], tabItem(TABS[index]));
        }
        services.quests().loadAll(player.getUniqueId()).thenAccept(progressById ->
                player.getScheduler().run(services.plugin(), task -> render(player, inventory, progressById), null));
    }

    private ItemStack tabItem(Section tab) {
        boolean active = tab == section;
        MessageKey name = switch (tab) {
            case DAILY -> MessageKey.GUI_QUESTS_SECTION_DAILY;
            case WEEKLY -> MessageKey.GUI_QUESTS_SECTION_WEEKLY;
            case PERMANENT -> MessageKey.GUI_QUESTS_SECTION_PERMANENT;
            case COMPLETED -> MessageKey.GUI_QUESTS_SECTION_COMPLETED;
        };
        MessageKey lore = switch (tab) {
            case DAILY -> MessageKey.GUI_QUESTS_SECTION_DAILY_LORE;
            case WEEKLY -> MessageKey.GUI_QUESTS_SECTION_WEEKLY_LORE;
            case PERMANENT -> MessageKey.GUI_QUESTS_SECTION_PERMANENT_LORE;
            case COMPLETED -> MessageKey.GUI_QUESTS_SECTION_COMPLETED_LORE;
        };
        Material icon = switch (tab) {
            case DAILY -> Material.CLOCK;
            case WEEKLY -> Material.SUNFLOWER;
            case PERMANENT -> Material.WRITTEN_BOOK;
            case COMPLETED -> Material.LIME_DYE;
        };
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(services.messages().render(lore));
        tooltip.add(services.messages().render(active
                ? MessageKey.GUI_QUESTS_TAB_ACTIVE : MessageKey.GUI_QUESTS_TAB_INACTIVE));
        return GuiItems.of(icon, services.messages().render(name), tooltip, active);
    }

    private void render(Player player, Inventory inventory, Map<String, QuestProgress> progressById) {
        List<QuestDefinition> assigned = services.quests().definitions().stream()
                .filter(definition -> progressById.containsKey(definition.id()))
                .sorted(Comparator.comparing(QuestDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        long completedCount = assigned.stream().filter(definition -> progressById.get(definition.id()).completed()).count();

        inventory.setItem(INFO_SLOT, GuiItems.of(Material.WRITABLE_BOOK,
                services.messages().render(MessageKey.GUI_QUESTS_INFO_ITEM),
                List.of(
                        services.messages().render(MessageKey.GUI_QUESTS_INFO_LORE, Map.of(
                                "tamamlanan", Long.toString(completedCount),
                                "toplam", Integer.toString(assigned.size())
                        )),
                        services.messages().render(MessageKey.GUI_QUESTS_INFO_PIN_HINT, Map.of(
                                "sayi", Integer.toString(QuestService.MAXIMUM_PINS_PER_TYPE)
                        ))
                )));

        List<QuestDefinition> shown = assigned.stream().filter(definition -> belongsToSection(definition, progressById)).toList();
        slotToQuestId.clear();
        ItemStack blank = GuiItems.filler();
        for (int index = 0; index < ENTRY_SLOTS.length; index++) {
            if (index >= shown.size()) {
                inventory.setItem(ENTRY_SLOTS[index], blank);
                continue;
            }
            QuestDefinition definition = shown.get(index);
            slotToQuestId.put(ENTRY_SLOTS[index], definition.id());
            boolean pinned = services.quests().cachedPins(player.getUniqueId(), definition.type()).contains(definition.id());
            inventory.setItem(ENTRY_SLOTS[index], questItem(definition, progressById.get(definition.id()), pinned));
        }
        if (shown.isEmpty()) {
            inventory.setItem(ENTRY_SLOTS[0], GuiItems.of(Material.GRAY_DYE,
                    services.messages().render(MessageKey.GUI_QUESTS_SECTION_EMPTY)));
        }
    }

    private boolean belongsToSection(QuestDefinition definition, Map<String, QuestProgress> progressById) {
        boolean completed = progressById.get(definition.id()).completed();
        if (section == Section.COMPLETED) {
            return completed;
        }
        if (completed) {
            return false;
        }
        return switch (section) {
            case DAILY -> definition.type() == QuestType.DAILY;
            case WEEKLY -> definition.type() == QuestType.WEEKLY;
            case PERMANENT -> definition.type() == QuestType.PERMANENT;
            case COMPLETED -> false;
        };
    }

    private ItemStack questItem(QuestDefinition definition, QuestProgress progress, boolean pinned) {
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_DESCRIPTION, Map.of("aciklama", definition.description())));
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE, Map.of(
                "ilerleme", progress.progress() + "/" + definition.target()
        )));
        lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_REWARD, Map.of(
                "xp", Long.toString(definition.rewardExperience()),
                "beceri", skillName(definition.skillId())
        )));
        if (progress.completed()) {
            lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_COMPLETED));
        } else {
            lore.add(services.messages().render(pinned
                    ? MessageKey.GUI_QUEST_ITEM_LORE_PINNED : MessageKey.GUI_QUEST_ITEM_LORE_UNPINNED));
        }
        MessageKey nameKey = progress.completed() ? MessageKey.GUI_QUEST_ITEM_COMPLETED : MessageKey.GUI_QUEST_ITEM;
        return GuiItems.of(definition.icon(), services.messages().render(nameKey, Map.of("gorev", definition.displayName())),
                lore, progress.completed() || pinned);
    }

    private String skillName(String skillId) {
        var definition = services.skills().settings().definitions().get(skillId);
        return definition == null ? skillId : definition.displayName();
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new TasksMenu(services));
            return;
        }
        for (int index = 0; index < TAB_SLOTS.length; index++) {
            if (TAB_SLOTS[index] == slot) {
                GuiManager.open(player, new QuestsMenu(services, TABS[index]));
                return;
            }
        }
        String questId = slotToQuestId.get(slot);
        if (questId != null) {
            togglePin(player, questId);
        }
    }

    /**
     * Clicking an unfinished quest pins it to (or unpins it from) the sidebar
     * section for its type. The board reopens afterwards so the glint and the
     * lore line reflect the new state immediately - the same reopen-to-refresh
     * pattern every other action menu in this codebase uses.
     */
    private void togglePin(Player player, String questId) {
        services.quests().togglePin(player.getUniqueId(), questId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    switch (result) {
                        case PINNED -> services.messages().send(player, MessageKey.QUEST_PINNED,
                                Map.of("gorev", questName(questId)));
                        case UNPINNED -> services.messages().send(player, MessageKey.QUEST_UNPINNED,
                                Map.of("gorev", questName(questId)));
                        case FULL -> services.messages().send(player, MessageKey.QUEST_PIN_FULL,
                                Map.of("sayi", Integer.toString(QuestService.MAXIMUM_PINS_PER_TYPE)));
                        case UNKNOWN_QUEST -> {
                            // Only reachable if the catalogue was reloaded between
                            // the render and the click; nothing useful to say.
                        }
                    }
                    GuiManager.open(player, new QuestsMenu(services, section));
                }, null));
    }

    private String questName(String questId) {
        QuestDefinition definition = services.quests().settings().definitions().get(questId);
        return definition == null ? questId : definition.displayName();
    }
}
