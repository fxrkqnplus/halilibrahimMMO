package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingTask;
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
import java.util.Set;

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
 *
 * <p><strong>Başlangıç comes first and gates the rest.</strong> Until every
 * onboarding task is done the other three active tabs are locked, and the panel
 * shows nothing but the starting list. A new player was previously handed three
 * pools of quests and a tutorial at the same time and had no way of telling
 * which of them was the one meant for them; there is now exactly one, and
 * finishing it is what opens the rest. Completed onboarding tasks stay visible
 * in the Tamamlananlar tab afterwards, so the first thing a player did on the
 * server does not silently disappear from their record.</p>
 */
public final class QuestsMenu implements GuiView {

    /**
     * Five tabs, of which only three are a {@link QuestType}: Başlangıç is the
     * onboarding list and Tamamlananlar cuts across everything.
     */
    public enum Section {
        BASLANGIC, DAILY, WEEKLY, PERMANENT, COMPLETED
    }

    private static final int[] TAB_SLOTS = {0, 9, 18, 27, 36};
    private static final Section[] TABS = {
            Section.BASLANGIC, Section.DAILY, Section.WEEKLY, Section.PERMANENT, Section.COMPLETED
    };
    /** Top right of the entry area, which is the one slot the tab column freed up. */
    private static final int INFO_SLOT = 8;
    private static final int BACK_SLOT = 45;
    private static final int[] DIVIDER_SLOTS = {1, 10, 19, 28, 37, 46};
    private static final int[] ENTRY_SLOTS = {
            2, 3, 4, 5, 6, 7,
            11, 12, 13, 14, 15, 16, 17,
            20, 21, 22, 23, 24, 25, 26,
            29, 30, 31, 32, 33, 34, 35,
            38, 39, 40, 41, 42, 43, 44,
            47, 48, 49, 50, 51, 52, 53
    };

    private final GuiServices services;
    private final Section section;
    private final Map<Integer, String> slotToQuestId = new LinkedHashMap<>();

    /**
     * Opens on Başlangıç, which is either the list a new player has to finish or
     * - once it is done - a short record of what they already did.
     */
    public QuestsMenu(GuiServices services) {
        this(services, Section.BASLANGIC);
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
            inventory.setItem(TAB_SLOTS[index], tabItem(player, TABS[index]));
        }
        services.quests().loadAll(player.getUniqueId()).thenAccept(progressById ->
                player.getScheduler().run(services.plugin(), task -> render(player, inventory, progressById), null));
    }

    /**
     * The starting list, drawn from the fixed {@link OnboardingTask} enum rather
     * than from the quest catalogue - these are not quests, they have no
     * objective counter, and pretending otherwise would mean inventing quest
     * definitions nobody can edit.
     */
    private void renderOnboarding(Player player, Inventory inventory) {
        Set<String> completed = services.onboarding().cachedCompleted(player.getUniqueId());
        ItemStack blank = GuiItems.filler();
        int index = 0;
        for (OnboardingTask task : OnboardingTask.values()) {
            boolean done = completed.contains(task.id());
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_REWARD_XP, Map.of(
                    "xp", Long.toString(task.rewardExperience())
            )));
            lore.add(services.messages().render(done
                    ? MessageKey.GUI_QUEST_ITEM_LORE_COMPLETED : MessageKey.GUI_QUESTS_ONBOARDING_PENDING));
            inventory.setItem(ENTRY_SLOTS[index++], GuiItems.of(
                    done ? Material.LIME_DYE : task.icon(),
                    services.messages().render(task.displayKey()), lore, done));
        }
        for (; index < ENTRY_SLOTS.length; index++) {
            inventory.setItem(ENTRY_SLOTS[index], blank);
        }
    }

    private ItemStack tabItem(Player player, Section tab) {
        boolean active = tab == section;
        boolean locked = isLocked(player, tab);
        MessageKey name = switch (tab) {
            case BASLANGIC -> MessageKey.GUI_QUESTS_SECTION_ONBOARDING;
            case DAILY -> MessageKey.GUI_QUESTS_SECTION_DAILY;
            case WEEKLY -> MessageKey.GUI_QUESTS_SECTION_WEEKLY;
            case PERMANENT -> MessageKey.GUI_QUESTS_SECTION_PERMANENT;
            case COMPLETED -> MessageKey.GUI_QUESTS_SECTION_COMPLETED;
        };
        MessageKey lore = switch (tab) {
            case BASLANGIC -> MessageKey.GUI_QUESTS_SECTION_ONBOARDING_LORE;
            case DAILY -> MessageKey.GUI_QUESTS_SECTION_DAILY_LORE;
            case WEEKLY -> MessageKey.GUI_QUESTS_SECTION_WEEKLY_LORE;
            case PERMANENT -> MessageKey.GUI_QUESTS_SECTION_PERMANENT_LORE;
            case COMPLETED -> MessageKey.GUI_QUESTS_SECTION_COMPLETED_LORE;
        };
        Material icon = switch (tab) {
            case BASLANGIC -> Material.OAK_SAPLING;
            case DAILY -> Material.CLOCK;
            case WEEKLY -> Material.SUNFLOWER;
            case PERMANENT -> Material.WRITTEN_BOOK;
            case COMPLETED -> Material.LIME_DYE;
        };
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(services.messages().render(lore));
        if (locked) {
            // A locked tab shows a barrier and says what opens it, rather than
            // being hidden: a player who cannot see the daily quests at all has
            // no idea they are coming.
            tooltip.add(services.messages().render(MessageKey.GUI_QUESTS_TAB_LOCKED));
            return GuiItems.of(Material.BARRIER, services.messages().render(name), tooltip, false);
        }
        tooltip.add(services.messages().render(active
                ? MessageKey.GUI_QUESTS_TAB_ACTIVE : MessageKey.GUI_QUESTS_TAB_INACTIVE));
        return GuiItems.of(icon, services.messages().render(name), tooltip, active);
    }

    /**
     * Whether this tab is still behind the starting quests.
     *
     * <p>Başlangıç is never locked, for the obvious reason, and neither is
     * Tamamlananlar - a player who has finished two starting tasks should be
     * able to see that they have.</p>
     */
    private boolean isLocked(Player player, Section tab) {
        if (tab == Section.BASLANGIC || tab == Section.COMPLETED) {
            return false;
        }
        return !OnboardingTask.allCompleted(services.onboarding().cachedCompleted(player.getUniqueId()));
    }

    private void render(Player player, Inventory inventory, Map<String, QuestProgress> progressById) {
        if (section == Section.BASLANGIC) {
            renderOnboarding(player, inventory);
            renderInfo(player, inventory, progressById);
            return;
        }
        List<QuestDefinition> assigned = services.quests().definitions().stream()
                .filter(definition -> progressById.containsKey(definition.id()))
                .sorted(Comparator.comparing(QuestDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        renderInfo(player, inventory, progressById);

        List<QuestDefinition> shown = assigned.stream().filter(definition -> belongsToSection(definition, progressById)).toList();
        slotToQuestId.clear();
        ItemStack blank = GuiItems.filler();
        int index = 0;
        for (; index < ENTRY_SLOTS.length && index < shown.size(); index++) {
            QuestDefinition definition = shown.get(index);
            slotToQuestId.put(ENTRY_SLOTS[index], definition.id());
            boolean pinned = services.quests().cachedPins(player.getUniqueId(), definition.type()).contains(definition.id());
            boolean dismissed = services.quests().cachedDismissals(player.getUniqueId()).contains(definition.id());
            inventory.setItem(ENTRY_SLOTS[index], questItem(definition, progressById.get(definition.id()), pinned, dismissed));
        }
        // A finished starting task belongs on the completed list as much as a
        // finished quest does: it is the first thing this player did here, and
        // dropping it from the record the moment it is done would be the one
        // place the board forgets something.
        if (section == Section.COMPLETED) {
            index = appendCompletedOnboarding(player, inventory, index);
        }
        boolean empty = index == 0;
        for (; index < ENTRY_SLOTS.length; index++) {
            inventory.setItem(ENTRY_SLOTS[index], blank);
        }
        if (empty) {
            inventory.setItem(ENTRY_SLOTS[0], GuiItems.of(Material.GRAY_DYE,
                    services.messages().render(MessageKey.GUI_QUESTS_SECTION_EMPTY)));
        }
    }

    /** @return the next free entry index after the completed starting tasks */
    private int appendCompletedOnboarding(Player player, Inventory inventory, int startIndex) {
        Set<String> completed = services.onboarding().cachedCompleted(player.getUniqueId());
        int index = startIndex;
        for (OnboardingTask task : OnboardingTask.values()) {
            if (!completed.contains(task.id()) || index >= ENTRY_SLOTS.length) {
                continue;
            }
            inventory.setItem(ENTRY_SLOTS[index++], GuiItems.of(Material.LIME_DYE,
                    services.messages().render(task.displayKey()),
                    List.of(
                            services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_REWARD_XP, Map.of(
                                    "xp", Long.toString(task.rewardExperience())
                            )),
                            services.messages().render(MessageKey.GUI_QUEST_ITEM_LORE_COMPLETED)
                    ), true));
        }
        return index;
    }

    /**
     * The counter in the corner, and - while the starting list is unfinished -
     * the one line explaining why three tabs are barriers.
     */
    private void renderInfo(Player player, Inventory inventory, Map<String, QuestProgress> progressById) {
        long assigned = services.quests().definitions().stream()
                .filter(definition -> progressById.containsKey(definition.id())).count();
        long completedCount = progressById.values().stream().filter(QuestProgress::completed).count();
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_QUESTS_INFO_LORE, Map.of(
                "tamamlanan", Long.toString(completedCount),
                "toplam", Long.toString(assigned)
        )));
        if (isLocked(player, Section.DAILY)) {
            lore.add(services.messages().render(MessageKey.GUI_QUESTS_INFO_ONBOARDING_GATE));
        } else {
            lore.add(services.messages().render(MessageKey.GUI_QUESTS_INFO_PIN_HINT, Map.of(
                    "sayi", Integer.toString(QuestService.MAXIMUM_PINS_PER_TYPE)
            )));
        }
        inventory.setItem(INFO_SLOT, GuiItems.of(Material.WRITABLE_BOOK,
                services.messages().render(MessageKey.GUI_QUESTS_INFO_ITEM), lore));
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
            case BASLANGIC, COMPLETED -> false;
        };
    }

    private ItemStack questItem(QuestDefinition definition, QuestProgress progress, boolean pinned, boolean dismissed) {
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
            lore.add(services.messages().render(dismissed
                    ? MessageKey.GUI_QUEST_ITEM_LORE_DISMISSED : MessageKey.GUI_QUEST_ITEM_LORE_DISMISS_HINT));
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
            if (TAB_SLOTS[index] != slot) {
                continue;
            }
            if (isLocked(player, TABS[index])) {
                services.messages().send(player, MessageKey.QUEST_ONBOARDING_REQUIRED);
                return;
            }
            GuiManager.open(player, new QuestsMenu(services, TABS[index]));
            return;
        }
        String questId = slotToQuestId.get(slot);
        if (questId == null) {
            return;
        }
        // A finished quest has nothing left to pin - the only useful thing to do
        // with it is to stop it taking up a panel line, which is a different
        // verb on the same click.
        if (section == Section.COMPLETED) {
            dismiss(player, questId);
            return;
        }
        togglePin(player, questId);
    }

    /**
     * Clicking a finished quest takes it off the panel for good.
     *
     * <p>Deliberately one-way rather than a toggle. A completed permanent quest
     * keeps its panel line forever otherwise, and "I have read this" is not a
     * state anybody wants to switch back off - so putting it back is not on
     * offer, and the lore line says so before the click.</p>
     */
    private void dismiss(Player player, String questId) {
        if (services.quests().cachedDismissals(player.getUniqueId()).contains(questId)) {
            return;
        }
        services.quests().dismiss(player.getUniqueId(), questId).thenAccept(dismissed ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (dismissed) {
                        services.messages().send(player, MessageKey.QUEST_DISMISSED, Map.of("gorev", questName(questId)));
                        services.sidebar().refresh(player);
                    }
                    GuiManager.open(player, new QuestsMenu(services, section));
                }, null));
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
