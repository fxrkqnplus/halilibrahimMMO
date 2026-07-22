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

/** Read-only display of the invoking player's global profile. */
public final class ProfileMenu implements GuiView {

    private static final int PROFILE_SLOT = 4;
    private static final int BACK_SLOT = 8;

    private final GuiServices services;

    public ProfileMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PROFILE_TITLE);
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.profiles().load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(services.plugin(), task -> {
                    String guc = Integer.toString(services.progression().cachedGlobalLevel(player.getUniqueId()));
                    Component lore = profile.selectedClass().isEmpty()
                            ? services.messages().render(MessageKey.GUI_PROFILE_ITEM_LORE_NO_CLASS, Map.of(
                                    "seviye", guc,
                                    "prestij", Integer.toString(profile.prestige())
                            ))
                            : services.messages().render(MessageKey.GUI_PROFILE_ITEM_LORE, Map.of(
                                    "seviye", guc,
                                    "prestij", Integer.toString(profile.prestige()),
                                    "sinif", services.classes().displayName(profile.selectedClass())
                            ));
                    inventory.setItem(PROFILE_SLOT, GuiItems.of(Material.PLAYER_HEAD,
                            services.messages().render(MessageKey.GUI_PROFILE_ITEM), List.of(lore)));
                }, null));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new ProgressionMenu(services));
        }
    }
}
