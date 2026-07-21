package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Cosmetic unlock status and equip action, mirroring {@code /kozmetik}/{@code /kozmetik sec}. */
public final class CosmeticsMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final Map<Integer, String> slotToCosmeticId = new LinkedHashMap<>();

    public CosmeticsMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_COSMETICS_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.cosmetics().unlockedIds(player.getUniqueId())
                .thenCompose(unlocked -> services.cosmetics().findEquipped(player.getUniqueId())
                        .thenAccept(equipped -> player.getScheduler().run(services.plugin(), task ->
                                render(inventory, unlocked, equipped), null)));
    }

    private void render(Inventory inventory, Set<String> unlocked, Optional<CosmeticDefinition> equipped) {
        List<CosmeticDefinition> definitions = services.cosmetics().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        String equippedId = equipped.map(CosmeticDefinition::id).orElse("");
        int slot = FIRST_SLOT;
        for (CosmeticDefinition definition : definitions) {
            boolean isUnlocked = unlocked.contains(definition.id());
            boolean isEquipped = definition.id().equals(equippedId);
            slotToCosmeticId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    definition.icon(),
                    services.messages().render(MessageKey.GUI_COSMETIC_ITEM, Map.of("kozmetik", definition.displayName())),
                    List.of(services.messages().render(isUnlocked ? MessageKey.GUI_COSMETIC_ITEM_LORE_UNLOCKED : MessageKey.GUI_COSMETIC_ITEM_LORE_LOCKED)),
                    isEquipped
            ));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new CommunityMenu(services));
            return;
        }
        String cosmeticId = slotToCosmeticId.get(slot);
        if (cosmeticId == null) {
            return;
        }
        services.cosmetics().equip(player.getUniqueId(), player.getName(), cosmeticId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    MessageKey key = switch (result.status()) {
                        case EQUIPPED -> MessageKey.COSMETIC_EQUIPPED;
                        case ALREADY_EQUIPPED -> MessageKey.COSMETIC_ALREADY_EQUIPPED;
                        case NOT_UNLOCKED -> MessageKey.COSMETIC_NOT_UNLOCKED;
                        case UNKNOWN_COSMETIC -> MessageKey.COSMETIC_UNKNOWN;
                    };
                    services.messages().send(player, key, Map.of("kozmetik", displayNameOrId(result.cosmeticId())));
                    GuiManager.open(player, new CosmeticsMenu(services));
                }, null));
    }

    private String displayNameOrId(String cosmeticId) {
        CosmeticDefinition definition = services.cosmetics().settings().definitions().get(cosmeticId);
        return definition == null ? cosmeticId : definition.displayName();
    }
}
