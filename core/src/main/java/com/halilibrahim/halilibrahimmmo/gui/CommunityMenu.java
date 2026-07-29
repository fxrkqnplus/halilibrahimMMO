package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** "Topluluk" category sub-hub: leaderboards, titles and cosmetics. */
public final class CommunityMenu implements GuiView {

    private static final int LEADERBOARDS_SLOT = 20;
    private static final int TITLES_SLOT = 22;
    private static final int COSMETICS_SLOT = 24;
    private static final int BACK_SLOT = 40;

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
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.PURPLE_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(LEADERBOARDS_SLOT, GuiItems.of(Material.BEACON,
                services.messages().render(MessageKey.GUI_MAIN_LEADERBOARDS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_LEADERBOARDS_LORE))));
        inventory.setItem(TITLES_SLOT, GuiItems.of(Material.NAME_TAG,
                services.messages().render(MessageKey.GUI_MAIN_TITLES),
                List.of(services.messages().render(MessageKey.GUI_MAIN_TITLES_LORE))));
        inventory.setItem(COSMETICS_SLOT, GuiItems.of(Material.FIREWORK_STAR,
                services.messages().render(MessageKey.GUI_MAIN_COSMETICS),
                List.of(services.messages().render(MessageKey.GUI_MAIN_COSMETICS_LORE))));
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
