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
import java.util.List;
import java.util.Map;

/**
 * Every online player, for {@code /haliladmin}.
 *
 * <p>Clicking one opens {@link AdminPlayerEditMenu} - the same editor the
 * all-players list opens. It used to open a read-only summary instead, which
 * meant staff dealing with somebody who was <em>right there</em> had to go back
 * out, into the all-players list, and find the same name again before they
 * could change anything. There was never a reason for the two lists to lead
 * anywhere different.</p>
 */
public final class AdminOnlinePlayersMenu implements GuiView {

    private static final int FIRST_SLOT = 0;
    private static final int BACK_SLOT = 53;
    /** The footer row is reserved, so entries stop one row short of the board. */
    private static final int MAXIMUM_LISTED = 45;

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
        GuiItems.listBoard(inventory, GuiItems.pane(Material.ORANGE_STAINED_GLASS_PANE));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        int slot = FIRST_SLOT;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= MAXIMUM_LISTED) {
                break;
            }
            slotToPlayerId.put(slot, online.getUniqueId());
            inventory.setItem(slot++, GuiItems.playerHead(
                    online,
                    services.messages().render(MessageKey.GUI_ADMIN_PLAYER_ENTRY, Map.of("oyuncu", online.getName())),
                    List.of(services.messages().render(MessageKey.GUI_ADMIN_PLAYER_ENTRY_LORE, Map.of(
                            "seviye", Integer.toString(services.progression().cachedGlobalLevel(online.getUniqueId())),
                            "sinif", services.profiles().cached(online.getUniqueId())
                                    .map(profile -> profile.selectedClass())
                                    .filter(classId -> !classId.isEmpty())
                                    .map(classId -> services.classes().displayName(classId))
                                    .orElse("-")
                    )))
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
            GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, target.getName(), true));
        }
    }
}
