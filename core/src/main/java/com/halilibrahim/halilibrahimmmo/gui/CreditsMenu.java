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
 * Who made this plugin, reachable from both the player hub ({@link AnaMenu})
 * and the admin panel ({@link AdminMenu}) - {@code fromAdmin} only decides
 * which of the two the back button returns to, since a
 * {@link GuiView} is rebuilt per open and has no navigation stack (§17).
 */
public final class CreditsMenu implements GuiView {

    private static final int PLUGIN_SLOT = 4;
    private static final int FURKAN_SLOT = 11;
    private static final int CLAUDE_SLOT = 15;
    private static final int THANKS_SLOT = 22;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final boolean fromAdmin;

    public CreditsMenu(GuiServices services, boolean fromAdmin) {
        this.services = services;
        this.fromAdmin = fromAdmin;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_CREDITS_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        ItemStack border = GuiItems.border();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, border);
        }
        inventory.setItem(PLUGIN_SLOT, GuiItems.of(
                Material.NETHER_STAR,
                services.messages().render(MessageKey.GUI_CREDITS_PLUGIN_ITEM),
                List.of(services.messages().render(MessageKey.VERSION_INFO, Map.of(
                        "surum", services.api().getPluginVersion(),
                        "paper_surumu", services.api().getSupportedMinecraftVersion()
                )))
        ));
        inventory.setItem(FURKAN_SLOT, GuiItems.of(
                Material.GOLDEN_HELMET,
                services.messages().render(MessageKey.GUI_CREDITS_AUTHOR_ITEM),
                List.of(
                        services.messages().render(MessageKey.GUI_CREDITS_AUTHOR_HANDLE),
                        services.messages().render(MessageKey.GUI_CREDITS_AUTHOR_ROLE)
                )
        ));
        inventory.setItem(CLAUDE_SLOT, GuiItems.of(
                Material.AMETHYST_SHARD,
                services.messages().render(MessageKey.GUI_CREDITS_CLAUDE_ITEM),
                List.of(
                        services.messages().render(MessageKey.GUI_CREDITS_CLAUDE_HANDLE),
                        services.messages().render(MessageKey.GUI_CREDITS_CLAUDE_ROLE)
                )
        ));
        inventory.setItem(THANKS_SLOT, GuiItems.of(
                Material.CAKE,
                services.messages().render(MessageKey.GUI_CREDITS_THANKS_ITEM),
                List.of(services.messages().render(MessageKey.GUI_CREDITS_THANKS_LORE))
        ));
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, fromAdmin ? new AdminMenu(services) : new AnaMenu(services));
        }
    }
}
