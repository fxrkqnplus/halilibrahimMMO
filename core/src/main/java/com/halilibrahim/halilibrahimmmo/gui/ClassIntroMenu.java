package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
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

/**
 * The onboarding class-introduction screen, opened immediately after
 * {@code /mmo aktif}: shows every class with a short flavor blurb and its
 * three abilities, so a first-time player can make an informed pick before
 * moving on to {@link ClassConfirmMenu}.
 */
public final class ClassIntroMenu implements GuiView {

    private static final int[] CLASS_SLOTS = {10, 12, 14, 16};

    private final GuiServices services;
    private final Map<Integer, String> slotToClassId = new LinkedHashMap<>();

    public ClassIntroMenu(GuiServices services) {
        this.services = services;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_CLASS_INTRO_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        List<PlayerClassDefinition> definitions = services.classes().definitions().stream()
                .sorted((left, right) -> left.displayName().compareToIgnoreCase(right.displayName())).toList();
        int index = 0;
        for (PlayerClassDefinition definition : definitions) {
            if (index >= CLASS_SLOTS.length) {
                break;
            }
            int slot = CLASS_SLOTS[index++];
            slotToClassId.put(slot, definition.id());
            List<Component> lore = new ArrayList<>();
            MessageKey blurb = introLoreKey(definition.id());
            if (blurb != null) {
                lore.add(services.messages().render(blurb));
            }
            services.abilities().definitions().stream()
                    .filter(ability -> ability.classId().equals(definition.id()))
                    .sorted((left, right) -> Integer.compare(left.requiredLevel(), right.requiredLevel()))
                    .forEach(ability -> lore.add(services.messages().render(MessageKey.GUI_CLASS_INTRO_ABILITY_LINE,
                            Map.of("kabiliyet", ability.displayName()))));
            inventory.setItem(slot, GuiItems.of(
                    Material.DIAMOND_SWORD,
                    services.messages().render(MessageKey.GUI_CLASS_ITEM, Map.of("sinif", definition.displayName())),
                    lore
            ));
        }
    }

    private MessageKey introLoreKey(String classId) {
        return switch (classId) {
            case "savasci" -> MessageKey.GUI_CLASS_INTRO_LORE_SAVASCI;
            case "avci" -> MessageKey.GUI_CLASS_INTRO_LORE_AVCI;
            case "madenci" -> MessageKey.GUI_CLASS_INTRO_LORE_MADENCI;
            case "oduncu" -> MessageKey.GUI_CLASS_INTRO_LORE_ODUNCU;
            default -> null;
        };
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        String classId = slotToClassId.get(slot);
        if (classId != null) {
            GuiManager.open(player, new ClassConfirmMenu(services, classId));
        }
    }
}
