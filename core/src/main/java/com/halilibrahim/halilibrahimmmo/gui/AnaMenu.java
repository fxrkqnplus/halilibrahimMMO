package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Central hub linking every player-facing menu. */
public final class AnaMenu implements GuiView {

    private static final int LEADERBOARDS_SLOT = 9;
    private static final int PROFILE_SLOT = 10;
    private static final int SKILLS_SLOT = 11;
    private static final int ABILITIES_SLOT = 12;
    private static final int CLASS_SLOT = 13;
    private static final int PRESTIGE_SLOT = 14;
    private static final int QUESTS_SLOT = 15;
    private static final int COLLECTIONS_SLOT = 16;
    private static final int JOURNAL_SLOT = 17;
    private static final int TITLES_SLOT = 18;
    private static final int COSMETICS_SLOT = 19;

    private final GuiServices services;

    public AnaMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_MAIN_TITLE);
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
        inventory.setItem(LEADERBOARDS_SLOT, GuiItems.of(Material.BEACON, services.messages().render(MessageKey.GUI_MAIN_LEADERBOARDS)));
        inventory.setItem(PROFILE_SLOT, GuiItems.of(Material.PLAYER_HEAD, services.messages().render(MessageKey.GUI_MAIN_PROFILE)));
        inventory.setItem(SKILLS_SLOT, GuiItems.of(Material.EXPERIENCE_BOTTLE, services.messages().render(MessageKey.GUI_MAIN_SKILLS)));
        inventory.setItem(ABILITIES_SLOT, GuiItems.of(Material.BLAZE_POWDER, services.messages().render(MessageKey.GUI_MAIN_ABILITIES)));
        inventory.setItem(CLASS_SLOT, GuiItems.of(Material.DIAMOND_SWORD, services.messages().render(MessageKey.GUI_MAIN_CLASS)));
        inventory.setItem(PRESTIGE_SLOT, GuiItems.of(Material.NETHER_STAR, services.messages().render(MessageKey.GUI_MAIN_PRESTIGE)));
        inventory.setItem(QUESTS_SLOT, GuiItems.of(Material.WRITTEN_BOOK, services.messages().render(MessageKey.GUI_MAIN_QUESTS)));
        inventory.setItem(COLLECTIONS_SLOT, GuiItems.of(Material.CHEST, services.messages().render(MessageKey.GUI_MAIN_COLLECTIONS)));
        inventory.setItem(JOURNAL_SLOT, GuiItems.of(Material.MAP, services.messages().render(MessageKey.GUI_MAIN_JOURNAL)));
        inventory.setItem(TITLES_SLOT, GuiItems.of(Material.NAME_TAG, services.messages().render(MessageKey.GUI_MAIN_TITLES)));
        inventory.setItem(COSMETICS_SLOT, GuiItems.of(Material.FIREWORK_STAR, services.messages().render(MessageKey.GUI_MAIN_COSMETICS)));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case LEADERBOARDS_SLOT -> GuiManager.open(player, new LeaderboardMenu(services));
            case PROFILE_SLOT -> GuiManager.open(player, new ProfileMenu(services));
            case SKILLS_SLOT -> GuiManager.open(player, new SkillsMenu(services));
            case ABILITIES_SLOT -> GuiManager.open(player, new AbilitiesMenu(services));
            case CLASS_SLOT -> GuiManager.open(player, new ClassMenu(services));
            case PRESTIGE_SLOT -> GuiManager.open(player, new PrestigeMenu(services));
            case QUESTS_SLOT -> GuiManager.open(player, new QuestsMenu(services));
            case COLLECTIONS_SLOT -> GuiManager.open(player, new CollectionsMenu(services));
            case JOURNAL_SLOT -> GuiManager.open(player, new JournalMenu(services));
            case TITLES_SLOT -> GuiManager.open(player, new TitlesMenu(services));
            case COSMETICS_SLOT -> GuiManager.open(player, new CosmeticsMenu(services));
            default -> {
            }
        }
    }
}
