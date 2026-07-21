package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lists every online player for {@code /haliladmin} - click one to view {@link AdminPlayerInfoMenu}. */
public final class AdminOnlinePlayersMenu implements GuiView {

    private static final int FIRST_SLOT = 0;
    private static final int BACK_SLOT = 53;
    private static final int MAXIMUM_LISTED = BACK_SLOT;

    private final GuiServices services;
    private final Map<Integer, java.util.UUID> slotToPlayerId = new LinkedHashMap<>();

    public AdminOnlinePlayersMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_ONLINE_PLAYERS_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        int slot = FIRST_SLOT;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= MAXIMUM_LISTED) {
                break;
            }
            slotToPlayerId.put(slot, online.getUniqueId());
            inventory.setItem(slot++, GuiItems.of(
                    Material.PLAYER_HEAD,
                    services.messages().render(MessageKey.GUI_ADMIN_PLAYER_ENTRY, Map.of("oyuncu", online.getName()))
            ));
        }
        if (slot == FIRST_SLOT) {
            inventory.setItem(FIRST_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ADMIN_NO_PLAYERS)));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminMenu(services));
            return;
        }
        java.util.UUID targetId = slotToPlayerId.get(slot);
        Player target = targetId == null ? null : Bukkit.getPlayer(targetId);
        if (target != null) {
            GuiManager.open(player, new AdminPlayerInfoMenu(services, target));
        }
    }
}
