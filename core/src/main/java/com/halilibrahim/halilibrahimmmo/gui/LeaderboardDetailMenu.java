package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardEntry;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Ranked top-N list for one leaderboard, opened from {@link LeaderboardMenu}.
 *
 * <p>Rebuilt from a bare 27-slot strip of identical player heads. The top three
 * places now read as a podium - a distinct material and a distinct name colour
 * each - and the viewer's own row is marked, which is the one row anybody
 * actually looks for. The board is framed and headed like every other screen in
 * the plugin so it stops being the odd one out.</p>
 */
public final class LeaderboardDetailMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 49;
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

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
            case REPUTATION -> MessageKey.GUI_LEADERBOARD_DETAIL_TITLE_REPUTATION;
        };
        return services.messages().render(key);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        CompletableFuture<List<LeaderboardEntry>> future = category != null
                ? services.leaderboards().top(category)
                : services.leaderboards().topSkill(skillId);
        future.thenAccept(entries -> player.getScheduler().run(services.plugin(), task -> render(player, inventory, entries), null));
    }

    private void render(Player player, Inventory inventory, List<LeaderboardEntry> entries) {
        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_LEADERBOARD_DETAIL_HEADER_ITEM),
                List.of(services.messages().render(MessageKey.GUI_LEADERBOARD_DETAIL_HEADER_LORE, Map.of(
                        "sayi", Integer.toString(entries.size())
                )))
        ));
        if (entries.isEmpty()) {
            inventory.setItem(ENTRY_SLOTS[0], GuiItems.of(Material.BARRIER,
                    services.messages().render(MessageKey.GUI_LEADERBOARD_EMPTY_ITEM)));
            return;
        }
        for (int index = 0; index < entries.size() && index < ENTRY_SLOTS.length; index++) {
            LeaderboardEntry entry = entries.get(index);
            boolean self = entry.playerName().equalsIgnoreCase(player.getName());
            inventory.setItem(ENTRY_SLOTS[index], GuiItems.of(
                    rankIcon(entry.rank()), entryName(entry, self), List.of(entryLore(entry)), self
            ));
        }
    }

    /** A podium for the top three, an ordinary head for everyone else. */
    private Material rankIcon(int rank) {
        return switch (rank) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.PLAYER_HEAD;
        };
    }

    /**
     * A template per place rather than one fed a colour, for the same reason
     * every other ranked/tiered label in this project is: the colour belongs in
     * {@code messages.yml} where it can be changed, and no placeholder can
     * carry one.
     */
    private Component entryName(LeaderboardEntry entry, boolean self) {
        Map<String, String> placeholders = Map.of(
                "sira", Integer.toString(entry.rank()),
                "oyuncu", entry.playerName()
        );
        if (self) {
            return services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_SELF, placeholders);
        }
        MessageKey key = switch (entry.rank()) {
            case 1 -> MessageKey.GUI_LEADERBOARD_ENTRY_FIRST;
            case 2 -> MessageKey.GUI_LEADERBOARD_ENTRY_SECOND;
            case 3 -> MessageKey.GUI_LEADERBOARD_ENTRY_THIRD;
            default -> MessageKey.GUI_LEADERBOARD_ENTRY_ITEM;
        };
        return services.messages().render(key, placeholders);
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
            // The one board whose numbers reach eight figures, so it is the one
            // that has to be readable at a glance rather than counted digit by digit.
            case REPUTATION -> services.messages().render(MessageKey.GUI_LEADERBOARD_ENTRY_REPUTATION_LORE, Map.of(
                    "sayginlik", NumberFormatter.compact(entry.primaryValue())
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
