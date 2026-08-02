package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.resourcepack.PackChecksum;
import com.halilibrahim.halilibrahimmmo.resourcepack.ResourcePackSettings;
import com.halilibrahim.halilibrahimmmo.resourcepack.ResourcePackSnapshot;
import com.halilibrahim.halilibrahimmmo.resourcepack.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only view of every online player's resource pack state.
 *
 * <p>Has no actions, for the same reason {@link AdminModCheckMenu} has none:
 * the rule set lives in {@code kaynak-paketi.yml} and is applied by the Yenile
 * button on {@link AdminMenu}, so there is exactly one way to change pack
 * behaviour.</p>
 *
 * <p>The summary shows the host and the first eight characters of the digest,
 * never the full URL. An address is long enough to push everything else off the
 * line, and a lore line is visible to whoever is standing behind the admin; the
 * console already logs the whole thing on every load.</p>
 */
public final class AdminResourcePackMenu implements GuiView {

    private static final int SUMMARY_SLOT = 4;
    private static final int FIRST_SLOT = 9;
    private static final int LAST_ENTRY_SLOT = 45;
    private static final int BACK_SLOT = 53;
    private static final int DIGEST_PREVIEW_LENGTH = 8;

    private final GuiServices services;

    public AdminResourcePackMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_RESOURCEPACK_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.listBoard(inventory, GuiItems.pane(Material.PURPLE_STAINED_GLASS_PANE));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));

        ResourcePackSettings settings = services.resourcePack().settings();
        if (!settings.usable()) {
            inventory.setItem(SUMMARY_SLOT, GuiItems.of(
                    Material.BARRIER,
                    services.messages().render(MessageKey.GUI_ADMIN_RESOURCEPACK_DISABLED)
            ));
            return;
        }

        int applied = 0;
        int pending = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (statusOf(online) == ResourcePackStatus.APPLIED) {
                applied++;
            } else if (statusOf(online) == ResourcePackStatus.PENDING) {
                pending++;
            }
        }

        inventory.setItem(SUMMARY_SLOT, GuiItems.of(
                Material.MAP,
                services.messages().render(MessageKey.GUI_ADMIN_RESOURCEPACK_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_RESOURCEPACK_SUMMARY, Map.of(
                        "host", settings.host(),
                        "ozet", PackChecksum.abbreviate(settings.sha1(), DIGEST_PREVIEW_LENGTH),
                        "zorunlu", services.messages().plain(
                                settings.required() ? MessageKey.GENERAL_YES : MessageKey.GENERAL_NO,
                                Map.of()),
                        "uygulandi", Integer.toString(applied),
                        "bekliyor", Integer.toString(pending)
                )))
        ));

        int slot = FIRST_SLOT;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= LAST_ENTRY_SLOT) {
                break;
            }
            inventory.setItem(slot++, entryItem(online));
        }
        if (slot == FIRST_SLOT) {
            inventory.setItem(FIRST_SLOT, GuiItems.of(
                    Material.BARRIER,
                    services.messages().render(MessageKey.GUI_ADMIN_NO_PLAYERS)
            ));
        }
    }

    /**
     * A player with no snapshot is shown as pending rather than as an error: it
     * only means the pack has not been offered yet, which is the normal state
     * for the first second after a join.
     */
    private ResourcePackStatus statusOf(Player online) {
        return services.resourcePack().snapshot(online.getUniqueId())
                .map(ResourcePackSnapshot::status)
                .orElse(ResourcePackStatus.PENDING);
    }

    private ItemStack entryItem(Player online) {
        Optional<ResourcePackSnapshot> snapshot = services.resourcePack().snapshot(online.getUniqueId());
        ResourcePackStatus status = snapshot.map(ResourcePackSnapshot::status).orElse(ResourcePackStatus.PENDING);
        String reported = snapshot.map(ResourcePackSnapshot::lastReportedStatus)
                .map(Enum::name)
                .orElse("?");

        MessageKey nameKey = switch (status) {
            case APPLIED -> MessageKey.GUI_ADMIN_RESOURCEPACK_ENTRY_APPLIED;
            case PENDING -> MessageKey.GUI_ADMIN_RESOURCEPACK_ENTRY_PENDING;
            case EXEMPT -> MessageKey.GUI_ADMIN_RESOURCEPACK_ENTRY_EXEMPT;
        };
        Material icon = switch (status) {
            case APPLIED -> Material.LIME_DYE;
            case PENDING -> Material.YELLOW_DYE;
            case EXEMPT -> Material.GRAY_DYE;
        };

        return GuiItems.of(
                icon,
                services.messages().render(nameKey, Map.of("oyuncu", online.getName())),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_RESOURCEPACK_ENTRY_LORE,
                        Map.of("durum", reported)))
        );
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminMenu(services));
        }
    }
}
