package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.modcheck.ModVerificationSettings;
import com.halilibrahim.halilibrahimmmo.modcheck.ModVerificationSnapshot;
import com.halilibrahim.halilibrahimmmo.modcheck.ModVerificationStatus;
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
 * Read-only view of every online player's Fabric mod verification state.
 *
 * <p>Deliberately has no actions: nothing here can kick a player or change a
 * setting. The rule set lives in {@code modlar.yml} and is applied by the
 * Yenile button on {@link AdminMenu}, so there is exactly one way to change
 * verification behaviour and it is the same way for every admin.</p>
 */
public final class AdminModCheckMenu implements GuiView {

    private static final int SUMMARY_SLOT = 4;
    private static final int FIRST_SLOT = 9;
    private static final int BACK_SLOT = 53;

    private final GuiServices services;

    public AdminModCheckMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_MODCHECK_TITLE);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));

        ModVerificationSettings settings = services.modVerification().settings();
        if (!settings.enabled()) {
            inventory.setItem(SUMMARY_SLOT, GuiItems.of(
                    Material.BARRIER,
                    services.messages().render(MessageKey.GUI_ADMIN_MODCHECK_DISABLED)
            ));
            return;
        }

        inventory.setItem(SUMMARY_SLOT, GuiItems.of(
                Material.COMMAND_BLOCK,
                services.messages().render(MessageKey.GUI_ADMIN_MODCHECK_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_MODCHECK_SUMMARY, Map.of(
                        "zorunlu", Integer.toString(settings.requiredMods().size()),
                        "yasakli", Integer.toString(settings.forbiddenMods().size())
                )))
        ));

        int slot = FIRST_SLOT;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= BACK_SLOT) {
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
     * A player with no snapshot at all is shown as pending rather than as an
     * error: it simply means the handshake has not started yet, which is the
     * normal state for the first tick after a join.
     */
    private ItemStack entryItem(Player online) {
        Optional<ModVerificationSnapshot> snapshot = services.modVerification().snapshot(online.getUniqueId());
        ModVerificationStatus status = snapshot.map(ModVerificationSnapshot::status).orElse(ModVerificationStatus.PENDING);
        String brand = snapshot.map(ModVerificationSnapshot::clientBrand).orElse("");
        int modCount = snapshot.map(ModVerificationSnapshot::reportedModCount).orElse(0);

        MessageKey nameKey = switch (status) {
            case VERIFIED -> MessageKey.GUI_ADMIN_MODCHECK_ENTRY_VERIFIED;
            case PENDING -> MessageKey.GUI_ADMIN_MODCHECK_ENTRY_PENDING;
            case EXEMPT -> MessageKey.GUI_ADMIN_MODCHECK_ENTRY_EXEMPT;
        };
        Material icon = switch (status) {
            case VERIFIED -> Material.LIME_DYE;
            case PENDING -> Material.YELLOW_DYE;
            case EXEMPT -> Material.GRAY_DYE;
        };

        return GuiItems.of(
                icon,
                services.messages().render(nameKey, Map.of("oyuncu", online.getName())),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_MODCHECK_ENTRY_LORE, Map.of(
                        "marka", brand.isBlank() ? "?" : brand,
                        "sayi", Integer.toString(modCount)
                )))
        );
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminMenu(services));
        }
    }
}
