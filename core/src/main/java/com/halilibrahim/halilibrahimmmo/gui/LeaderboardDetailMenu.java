package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardEntry;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Ranked top-N list for one leaderboard, opened from {@link LeaderboardMenu}. */
public final class LeaderboardDetailMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int LAST_SLOT = 25;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final LeaderboardCategory category;
    private final String skillId;
    private final String skillDisplayName;

    public LeaderboardDetailMenu(GuiServices services, LeaderboardCategory category) {
        this.services = services;
        this.category = category;
        this.skillId = null;
        this.skillDisplayName = null;
    }

    public LeaderboardDetailMenu(GuiServices services, String skillId, String skillDisplayName) {
        this.services = services;
        this.category = null;
        this.skillId = skillId;
        this.skillDisplayName = skillDisplayName;
    }

    @Override
    public Component title() {
        if (category == null) {
            return services.messages().render(MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_SKILL, Map.of("yetenek", skillDisplayName));
        }
        MessageKey key = switch (category) {
            case GLOBAL_LEVEL -> MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_LEVEL;
            case PRESTIGE -> MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_PRESTIGE;
            case QUEST_COMPLETIONS -> MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_QUESTS;
            case COLLECTION_ENTRIES -> MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_COLLECTIONS;
        };
        return services.messages().render(key);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        CompletableFuture<List<LeaderboardEntry>> future = category != null
                ? services.leaderboards().top(category)
                : services.leaderboards().topSkill(skillId);
        future.thenAccept(entries -> player.getScheduler().run(services.plugin(), task -> {
            if (entries.isEmpty()) {
                inventory.setItem(FIRST_SLOT, GuiItems.of(Material.PLAYER_HEAD, services.messages().render(MessageKey.GUI_LEADERBOARD_EMPTY_ITEM)));
                return;
            }
            int slot = FIRST_SLOT;
            for (LeaderboardEntry entry : entries) {
                if (slot > LAST_SLOT) {
                    break;
                }
                inventory.setItem(slot++, GuiItems.of(Material.PLAYER_HEAD, entryName(entry), List.of(entryLore(entry))));
            }
        }, null));
    }

    private Component entryName(LeaderboardEntry entry) {
        return services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_ITEM, Map.of(
                "sira", Integer.toString(entry.rank()),
                "oyuncu", entry.playerName()
        ));
    }

    private Component entryLore(LeaderboardEntry entry) {
        if (category == null) {
            return levelLore(entry);
        }
        return switch (category) {
            case GLOBAL_LEVEL -> gucLore(entry);
            case PRESTIGE -> services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_PRESTIGE_LORE, Map.of(
                    "prestij", Long.toString(entry.primaryValue())
            ));
            case QUEST_COMPLETIONS -> services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_QUEST_LORE, Map.of(
                    "sayi", Long.toString(entry.primaryValue())
            ));
            case COLLECTION_ENTRIES -> services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_COLLECTION_LORE, Map.of(
                    "sayi", Long.toString(entry.primaryValue())
            ));
        };
    }

    /** Skill boards only - {@code GLOBAL_LEVEL} moved to {@link #gucLore(LeaderboardEntry)} since its secondary metric is prestige, not XP. */
    private Component levelLore(LeaderboardEntry entry) {
        return services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_LEVEL_LORE, Map.of(
                "seviye", Long.toString(entry.primaryValue()),
                "xp", Long.toString(entry.secondaryValue())
        ));
    }

    private Component gucLore(LeaderboardEntry entry) {
        return services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_GUC_LORE, Map.of(
                "seviye", Long.toString(entry.primaryValue()),
                "prestij", Long.toString(entry.secondaryValue())
        ));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new LeaderboardMenu(services));
        }
    }
}
