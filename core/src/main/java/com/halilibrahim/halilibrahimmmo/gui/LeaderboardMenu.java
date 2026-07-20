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
import java.util.Map;

/** Hub listing every fixed leaderboard category plus one board per configured skill. */
public final class LeaderboardMenu implements GuiView {

    private static final int LEVEL_SLOT = 10;
    private static final int PRESTIGE_SLOT = 11;
    private static final int QUEST_SLOT = 12;
    private static final int COLLECTION_SLOT = 13;
    private static final int FIRST_SKILL_SLOT = 14;
    private static final int BACK_SLOT = 26;

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
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));

        slotToCategory.put(LEVEL_SLOT, LeaderboardCategory.GLOBAL_LEVEL);
        slotToCategory.put(PRESTIGE_SLOT, LeaderboardCategory.PRESTIGE);
        slotToCategory.put(QUEST_SLOT, LeaderboardCategory.QUEST_COMPLETIONS);
        slotToCategory.put(COLLECTION_SLOT, LeaderboardCategory.COLLECTION_ENTRIES);

        inventory.setItem(LEVEL_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE, services.messages().render(MessageKey.GUI_LEADERBOARD_CATEGORY_LEVEL)));
        inventory.setItem(PRESTIGE_SLOT, GuiItems.of(Material.NETHER_STAR, services.messages().render(MessageKey.GUI_LEADERBOARD_CATEGORY_PRESTIGE)));
        inventory.setItem(QUEST_SLOT, GuiItems.of(Material.WRITTEN_BOOK, services.messages().render(MessageKey.GUI_LEADERBOARD_CATEGORY_QUESTS)));
        inventory.setItem(COLLECTION_SLOT, GuiItems.of(Material.CHEST, services.messages().render(MessageKey.GUI_LEADERBOARD_CATEGORY_COLLECTIONS)));

        int slot = FIRST_SKILL_SLOT;
        for (SkillDefinition definition : services.skills().definitions()) {
            slotToSkillId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    Material.EXPERIENCE_BOTTLE,
                    services.messages().render(MessageKey.GUI_LEADERBOARD_SKILL_ITEM, Map.of("yetenek", definition.displayName()))
            ));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
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
