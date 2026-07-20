package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
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

/** Click-to-activate view over every configured ability, mirroring {@code /kabiliyet}. */
public final class AbilitiesMenu implements GuiView {

    private static final int FIRST_SLOT = 10;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final Map<Integer, String> slotToAbilityId = new LinkedHashMap<>();

    public AbilitiesMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ABILITIES_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        List<AbilityDefinition> definitions = services.abilities().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int slot = FIRST_SLOT;
        for (AbilityDefinition definition : definitions) {
            slotToAbilityId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(
                    Material.BLAZE_POWDER,
                    services.messages().render(MessageKey.GUI_ABILITY_ITEM, Map.of("kabiliyet", definition.displayName())),
                    List.of(services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE, Map.of(
                            "seviye", Integer.toString(definition.requiredLevel()),
                            "enerji", Integer.toString(definition.energyCost()),
                            "bekleme", Long.toString(definition.cooldownMillis() / 1000L)
                    )))
            ));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AnaMenu(services));
            return;
        }
        String abilityId = slotToAbilityId.get(slot);
        if (abilityId != null) {
            services.abilities().activate(player, abilityId, services.energy());
        }
    }
}
