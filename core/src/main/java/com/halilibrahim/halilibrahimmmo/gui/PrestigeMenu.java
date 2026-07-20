package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeResult;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Prestige status display plus a confirm button, mirroring {@code /prestij}/{@code /prestij onayla}. */
public final class PrestigeMenu implements GuiView {

    private static final int INFO_SLOT = 3;
    private static final int CONFIRM_SLOT = 5;
    private static final int BACK_SLOT = 8;

    private final GuiServices services;

    public PrestigeMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_PRESTIGE_TITLE);
    }

    @Override
    public int size() {
        return 9;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(CONFIRM_SLOT, GuiItems.of(Material.TOTEM_OF_UNDYING,
                services.messages().render(MessageKey.GUI_PRESTIGE_CONFIRM_ITEM),
                List.of(services.messages().render(MessageKey.GUI_PRESTIGE_CONFIRM_ITEM_LORE))));
        services.profiles().load(player.getUniqueId(), player.getName()).thenAccept(profile ->
                player.getScheduler().run(services.plugin(), task -> inventory.setItem(INFO_SLOT, GuiItems.of(
                        Material.NETHER_STAR,
                        services.messages().render(MessageKey.GUI_PRESTIGE_INFO_ITEM),
                        List.of(services.messages().render(MessageKey.GUI_PRESTIGE_INFO_ITEM_LORE, Map.of(
                                "seviye", Integer.toString(profile.globalLevel()),
                                "gereken", Integer.toString(services.progression().settings().curve().maximumLevel()),
                                "prestij", Integer.toString(profile.prestige()),
                                "maksimum", Integer.toString(services.prestige().settings().maximumPrestige())
                        )))
                )), null));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        if (slot != CONFIRM_SLOT) {
            return;
        }
        services.prestige().prestige(player.getUniqueId(), player.getName()).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    MessageKey key = switch (result.status()) {
                        case PRESTIGED -> MessageKey.PRESTIGE_SUCCESS;
                        case LEVEL_REQUIRED -> MessageKey.PRESTIGE_LEVEL_REQUIRED;
                        case MAXIMUM_REACHED -> MessageKey.PRESTIGE_MAXIMUM;
                    };
                    services.messages().send(player, key, Map.of(
                            "prestij", Integer.toString(result.prestige()),
                            "maksimum", Integer.toString(services.prestige().settings().maximumPrestige())
                    ));
                    if (result.status() == PrestigeResult.Status.PRESTIGED) {
                        services.feedback().celebratePrestige(player, result.prestige());
                        services.titles().evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                        services.cosmetics().evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
                    }
                    GuiManager.open(player, new PrestigeMenu(services));
                }, null));
    }

    private void notifyTitles(Player player, List<TitleDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(services.plugin(), task -> unlocked.forEach(definition ->
                services.messages().send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(services.plugin(), task -> unlocked.forEach(definition ->
                services.messages().send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }
}
