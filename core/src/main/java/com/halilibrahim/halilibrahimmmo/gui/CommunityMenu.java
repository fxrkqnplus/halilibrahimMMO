package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** "Topluluk" category sub-hub: leaderboards, titles and cosmetics. */
public final class CommunityMenu implements GuiView {

    private static final int LEADERBOARDS_SLOT = 10;
    private static final int TITLES_SLOT = 12;
    private static final int COSMETICS_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;

    public CommunityMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_COMMUNITY_TITLE);
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
        inventory.setItem(TITLES_SLOT, GuiItems.of(Material.NAME_TAG, services.messages().render(MessageKey.GUI_MAIN_TITLES)));
        inventory.setItem(COSMETICS_SLOT, GuiItems.of(Material.FIREWORK_STAR, services.messages().render(MessageKey.GUI_MAIN_COSMETICS)));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case LEADERBOARDS_SLOT -> GuiManager.open(player, new LeaderboardMenu(services));
            case TITLES_SLOT -> GuiManager.open(player, new TitlesMenu(services));
            case COSMETICS_SLOT -> GuiManager.open(player, new CosmeticsMenu(services));
            case BACK_SLOT -> GuiManager.open(player, new AnaMenu(services));
            default -> {
            }
        }
    }
}
