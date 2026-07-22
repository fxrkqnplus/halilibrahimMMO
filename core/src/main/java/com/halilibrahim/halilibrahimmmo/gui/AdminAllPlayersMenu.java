package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.activation.ActivatedPlayer;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
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
 * Lists every registered (activated) player, online or offline - click one to
 * open {@link AdminPlayerEditMenu}. Complements {@link AdminOnlinePlayersMenu},
 * which stays online-only and read-only.
 */
public final class AdminAllPlayersMenu implements GuiView {

    private static final int FIRST_SLOT = 0;
    private static final int BACK_SLOT = 53;
    private static final int MAXIMUM_LISTED = BACK_SLOT;

    private final GuiServices services;
    private final Map<Integer, ActivatedPlayer> slotToPlayer = new LinkedHashMap<>();

    public AdminAllPlayersMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_ALL_PLAYERS_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.activation().findAllActivated().thenAccept(players -> player.getScheduler().run(services.plugin(), task -> {
            if (players.isEmpty()) {
                inventory.setItem(FIRST_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ADMIN_NO_PLAYERS)));
                return;
            }
            int slot = FIRST_SLOT;
            for (ActivatedPlayer activated : players) {
                if (slot >= MAXIMUM_LISTED) {
                    break;
                }
                slotToPlayer.put(slot, activated);
                inventory.setItem(slot++, GuiItems.of(
                        Material.PLAYER_HEAD,
                        services.messages().render(MessageKey.GUI_ADMIN_PLAYER_ENTRY, Map.of("oyuncu", activated.playerName())),
                        List.of(services.messages().render(MessageKey.GUI_ADMIN_ALL_PLAYERS_LORE))
                ));
            }
        }, null));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminMenu(services));
            return;
        }
        ActivatedPlayer target = slotToPlayer.get(slot);
        if (target != null) {
            GuiManager.open(player, new AdminPlayerEditMenu(services, target.playerId(), target.playerName()));
        }
    }
}
