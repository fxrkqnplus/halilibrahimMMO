package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cosmetic unlock status and equip action - the only way to equip one, opened
 * by {@code /kozmetik}.
 *
 * <p>Rebuilt alongside {@link TitlesMenu} and to exactly the same layout: the
 * two screens are the same system over the same unlock model, and a player
 * moving between them should not have to re-learn where anything is. A locked
 * cosmetic shows its requirement and not its name, for the same reason.</p>
 */
public final class CosmeticsMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int BACK_SLOT = 49;
    private static final int[] ENTRY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

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
        return 54;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.border(), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        services.cosmetics().unlockedIds(player.getUniqueId())
                .thenCompose(unlocked -> services.cosmetics().findEquipped(player.getUniqueId())
                        .thenAccept(equipped -> player.getScheduler().run(services.plugin(), task ->
                                render(player, inventory, unlocked, equipped), null)));
    }

    private void render(Player player, Inventory inventory, Set<String> unlocked, Optional<CosmeticDefinition> equipped) {
        List<CosmeticDefinition> definitions = services.cosmetics().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        String equippedId = equipped.map(CosmeticDefinition::id).orElse("");

        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(
                player,
                services.messages().render(MessageKey.GUI_COSMETICS_HEADER_ITEM),
                List.of(services.messages().render(MessageKey.GUI_COSMETICS_HEADER_LORE, Map.of(
                        "acik", Integer.toString(unlocked.size()),
                        "toplam", Integer.toString(definitions.size())
                )))
        ));

        slotToCosmeticId.clear();
        for (int index = 0; index < ENTRY_SLOTS.length && index < definitions.size(); index++) {
            CosmeticDefinition definition = definitions.get(index);
            boolean isUnlocked = unlocked.contains(definition.id());
            if (isUnlocked) {
                slotToCosmeticId.put(ENTRY_SLOTS[index], definition.id());
            }
            inventory.setItem(ENTRY_SLOTS[index], entryItem(definition, isUnlocked, definition.id().equals(equippedId)));
        }
    }

    private ItemStack entryItem(CosmeticDefinition definition, boolean unlocked, boolean equippedNow) {
        if (!unlocked) {
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().render(MessageKey.GUI_COSMETIC_ITEM_LORE_LOCKED));
            lore.add(UnlockRequirementLore.of(services, definition.condition()));
            return GuiItems.of(Material.GRAY_DYE, services.messages().render(MessageKey.GUI_COSMETIC_ITEM_LOCKED), lore);
        }
        return GuiItems.of(
                definition.icon(),
                services.messages().render(MessageKey.GUI_COSMETIC_ITEM, Map.of("kozmetik", definition.displayName())),
                List.of(services.messages().render(equippedNow
                        ? MessageKey.GUI_COSMETIC_ITEM_LORE_EQUIPPED : MessageKey.GUI_COSMETIC_ITEM_LORE_UNLOCKED)),
                equippedNow,
                definition.model()
        );
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
