package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * {@code /haliladmin}'s hub screen - the only entry point into the plugin's
 * administrative surface now that {@code /rpg}'s chat-argument dispatch is
 * gone (see {@code AdminCommand}). Every action here calls the exact same
 * service methods the old {@code SubCommand}s did, just triggered by a click
 * instead of parsed chat arguments.
 */
public final class AdminMenu implements GuiView {

    private static final int RELOAD_SLOT = 11;
    private static final int DEBUG_SLOT = 13;
    private static final int VERSION_SLOT = 15;
    private static final int ONLINE_PLAYERS_SLOT = 22;

    private final GuiServices services;

    public AdminMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_TITLE);
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
        inventory.setItem(RELOAD_SLOT, GuiItems.of(
                Material.EMERALD,
                services.messages().render(MessageKey.GUI_ADMIN_RELOAD_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_RELOAD_LORE))
        ));
        boolean debugEnabled = services.config().coreConfig().debugMode();
        inventory.setItem(DEBUG_SLOT, GuiItems.of(
                Material.REDSTONE_TORCH,
                services.messages().render(MessageKey.GUI_ADMIN_DEBUG_ITEM),
                List.of(services.messages().render(debugEnabled ? MessageKey.GUI_ADMIN_DEBUG_LORE_ON : MessageKey.GUI_ADMIN_DEBUG_LORE_OFF)),
                debugEnabled
        ));
        inventory.setItem(VERSION_SLOT, GuiItems.of(
                Material.BOOK,
                services.messages().render(MessageKey.GUI_ADMIN_VERSION_ITEM),
                List.of(services.messages().render(MessageKey.VERSION_INFO, Map.of(
                        "surum", services.api().getPluginVersion(),
                        "paper_surumu", services.api().getSupportedMinecraftVersion()
                )))
        ));
        inventory.setItem(ONLINE_PLAYERS_SLOT, GuiItems.of(
                Material.PLAYER_HEAD,
                services.messages().render(MessageKey.GUI_ADMIN_ONLINE_PLAYERS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_ONLINE_PLAYERS_LORE))
        ));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        switch (slot) {
            case RELOAD_SLOT -> {
                boolean success = services.api().reload();
                services.messages().send(player, success ? MessageKey.RELOAD_SUCCESS : MessageKey.RELOAD_FAILURE);
                GuiManager.open(player, new AdminMenu(services));
            }
            case DEBUG_SLOT -> {
                boolean newState = !services.config().coreConfig().debugMode();
                services.config().setDebugModeOverride(newState);
                services.messages().send(player, newState ? MessageKey.DEBUG_ENABLED : MessageKey.DEBUG_DISABLED);
                GuiManager.open(player, new AdminMenu(services));
            }
            case ONLINE_PLAYERS_SLOT -> GuiManager.open(player, new AdminOnlinePlayersMenu(services));
            default -> {
            }
        }
    }
}
