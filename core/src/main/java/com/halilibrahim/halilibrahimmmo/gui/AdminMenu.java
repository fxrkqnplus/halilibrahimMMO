package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * {@code /haliladmin}'s hub screen - the only entry point into the plugin's
 * administrative surface.
 *
 * <p>Built in the same visual language as {@code /menü}: a framed 45-slot
 * board, a personal header reading the live server state, and every action
 * carrying a line explaining what it does before it is clicked. Staff screens
 * used to be the plain 27-slot grid this project started with, which made the
 * one screen where a misclick actually costs something the least legible screen
 * in the plugin.</p>
 */
public final class AdminMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int RELOAD_SLOT = 19;
    private static final int DEBUG_SLOT = 21;
    private static final int ONLINE_PLAYERS_SLOT = 23;
    private static final int ALL_PLAYERS_SLOT = 25;
    private static final int BOSS_SLOT = 30;
    private static final int VERSION_SLOT = 32;
    private static final int CREDITS_SLOT = 40;

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
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_ADMIN_HEADER_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_HEADER_LORE, Map.of(
                        "surum", services.api().getPluginVersion(),
                        "oyuncu", Integer.toString(Bukkit.getOnlinePlayers().size())
                )))
        ));

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
        inventory.setItem(ONLINE_PLAYERS_SLOT, GuiItems.of(
                Material.PLAYER_HEAD,
                services.messages().render(MessageKey.GUI_ADMIN_ONLINE_PLAYERS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_ONLINE_PLAYERS_LORE))
        ));
        inventory.setItem(ALL_PLAYERS_SLOT, GuiItems.of(
                Material.BOOKSHELF,
                services.messages().render(MessageKey.GUI_ADMIN_ALL_PLAYERS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_ALL_PLAYERS_LORE))
        ));
        inventory.setItem(BOSS_SLOT, GuiItems.of(
                Material.WITHER_SKELETON_SKULL,
                services.messages().render(MessageKey.GUI_ADMIN_BOSS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_BOSS_LORE))
        ));
        inventory.setItem(VERSION_SLOT, GuiItems.of(
                Material.BOOK,
                services.messages().render(MessageKey.GUI_ADMIN_VERSION_ITEM),
                List.of(services.messages().render(MessageKey.VERSION_INFO, Map.of(
                        "surum", services.api().getPluginVersion(),
                        "paper_surumu", services.api().getSupportedMinecraftVersion()
                )))
        ));
        inventory.setItem(CREDITS_SLOT, GuiItems.of(
                Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_ADMIN_CREDITS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_CREDITS_LORE))
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
            case ALL_PLAYERS_SLOT -> GuiManager.open(player, new AdminAllPlayersMenu(services));
            case BOSS_SLOT -> spawnBoss(player);
            case CREDITS_SLOT -> GuiManager.open(player, new CreditsMenu(services, true));
            default -> {
            }
        }
    }

    /**
     * Spawns the next world boss immediately instead of waiting out the
     * interval. The boss still picks its own archetype, rarity and location, so
     * this is a "start it now" button rather than a way to hand-place a mystic
     * boss on request.
     */
    private void spawnBoss(Player player) {
        services.bosses().spawnNow().ifPresentOrElse(
                name -> {
                    services.messages().send(player, MessageKey.ADMIN_BOSS_SPAWNED, Map.of("isim", name));
                    services.logger().info("Yonetici " + player.getName() + " elle bir dunya bossu cagirdi: " + name + ".");
                },
                () -> services.messages().send(player, MessageKey.ADMIN_BOSS_NO_ANCHOR)
        );
        player.closeInventory();
    }
}
