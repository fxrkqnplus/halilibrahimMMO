package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.skills.SkillDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hub listing every fixed leaderboard category plus one board per configured
 * skill.
 *
 * <p>Rebuilt from a 27-slot grid where the six skill boards ran from slot 15
 * straight through the back button's slot and out of the inventory, and where
 * every board - Güç, prestij, quests, collections and all six skills - was
 * drawn as the same experience bottle, so the icons carried no information at
 * all. Each fixed category now has an icon that says what it ranks, each skill
 * board uses that skill's own {@code simge} from {@code skills.yml}, and every
 * entry explains itself on hover.</p>
 */
public final class LeaderboardMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int SKILL_SECTION_SLOT = 18;
    private static final int BACK_SLOT = 40;
    /** Five fixed boards, centred on the first interior row. */
    private static final int[] CATEGORY_SLOTS = {11, 12, 13, 14, 15};
    /** Room for up to fourteen configured skills before a board would be dropped. */
    private static final int[] SKILL_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final LeaderboardCategory[] CATEGORIES = {
            LeaderboardCategory.GLOBAL_LEVEL, LeaderboardCategory.PRESTIGE, LeaderboardCategory.QUEST_COMPLETIONS,
            LeaderboardCategory.COLLECTION_ENTRIES, LeaderboardCategory.WEALTH
    };

    private final GuiServices services;
    private final Map<Integer, LeaderboardCategory> slotToCategory = new LinkedHashMap<>();
    private final Map<Integer, String> slotToSkillId = new LinkedHashMap<>();

    public LeaderboardMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_LEADERBOARDS_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_LEADERBOARDS_HEADER_ITEM),
                List.of(services.messages().render(MessageKey.GUI_LEADERBOARDS_HEADER_LORE, Map.of(
                        "sayi", Integer.toString(services.leaderboards().settings().entryLimit())
                )))
        ));

        slotToCategory.clear();
        for (int index = 0; index < CATEGORIES.length; index++) {
            LeaderboardCategory category = CATEGORIES[index];
            int slot = CATEGORY_SLOTS[index];
            slotToCategory.put(slot, category);
            inventory.setItem(slot, GuiItems.of(
                    icon(category),
                    services.messages().render(name(category)),
                    List.of(
                            services.messages().render(lore(category)),
                            services.messages().render(MessageKey.GUI_LEADERBOARD_OPEN_HINT)
                    )
            ));
        }

        inventory.setItem(SKILL_SECTION_SLOT, GuiItems.of(
                Material.BOOKSHELF, services.messages().render(MessageKey.GUI_LEADERBOARD_SKILL_SECTION)
        ));
        slotToSkillId.clear();
        int index = 0;
        for (SkillDefinition definition : services.skills().definitions()) {
            if (index >= SKILL_SLOTS.length) {
                break;
            }
            int slot = SKILL_SLOTS[index++];
            slotToSkillId.put(slot, definition.id());
            inventory.setItem(slot, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_LEADERBOARD_SKILL_ITEM, Map.of("yetenek", definition.displayName())),
                    List.of(
                            services.messages().render(MessageKey.GUI_LEADERBOARD_SKILL_LORE, Map.of("yetenek", definition.displayName())),
                            services.messages().render(MessageKey.GUI_LEADERBOARD_OPEN_HINT)
                    )
            ));
        }
    }

    /** One icon per board, chosen to say what the board measures before its label is read. */
    private Material icon(LeaderboardCategory category) {
        return switch (category) {
            case GLOBAL_LEVEL -> Material.NETHERITE_INGOT;
            case PRESTIGE -> Material.NETHER_STAR;
            case QUEST_COMPLETIONS -> Material.FILLED_MAP;
            case COLLECTION_ENTRIES -> Material.ENDER_CHEST;
            case WEALTH -> Material.DIAMOND;
        };
    }

    private MessageKey name(LeaderboardCategory category) {
        return switch (category) {
            case GLOBAL_LEVEL -> MessageKey.GUI_LEADERBOARD_CATEGORY_LEVEL;
            case PRESTIGE -> MessageKey.GUI_LEADERBOARD_CATEGORY_PRESTIGE;
            case QUEST_COMPLETIONS -> MessageKey.GUI_LEADERBOARD_CATEGORY_QUESTS;
            case COLLECTION_ENTRIES -> MessageKey.GUI_LEADERBOARD_CATEGORY_COLLECTIONS;
            case WEALTH -> MessageKey.GUI_LEADERBOARD_CATEGORY_WEALTH;
        };
    }

    private MessageKey lore(LeaderboardCategory category) {
        return switch (category) {
            case GLOBAL_LEVEL -> MessageKey.GUI_LEADERBOARD_CATEGORY_LEVEL_LORE;
            case PRESTIGE -> MessageKey.GUI_LEADERBOARD_CATEGORY_PRESTIGE_LORE;
            case QUEST_COMPLETIONS -> MessageKey.GUI_LEADERBOARD_CATEGORY_QUESTS_LORE;
            case COLLECTION_ENTRIES -> MessageKey.GUI_LEADERBOARD_CATEGORY_COLLECTIONS_LORE;
            case WEALTH -> MessageKey.GUI_LEADERBOARD_CATEGORY_WEALTH_LORE;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new CommunityMenu(services));
            return;
        }
        LeaderboardCategory category = slotToCategory.get(slot);
        if (category != null) {
            GuiManager.open(player, new LeaderboardDetailMenu(services, category));
            return;
        }
        String skillId = slotToSkillId.get(slot);
        if (skillId != null) {
            String displayName = services.skills().definitions().stream()
                    .filter(definition -> definition.id().equals(skillId))
                    .map(SkillDefinition::displayName)
                    .findFirst()
                    .orElse(skillId);
            GuiManager.open(player, new LeaderboardDetailMenu(services, skillId, displayName));
        }
    }
}
