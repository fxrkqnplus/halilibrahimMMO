package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.PlayerClassDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Force-sets a registered (possibly offline) player's class, bypassing the change cooldown - opened from {@link AdminPlayerEditMenu}. */
public final class AdminClassPickerMenu implements GuiView {

    private static final int FIRST_CLASS_SLOT = 10;
    private static final int CLEAR_SLOT = 22;
    private static final int BACK_SLOT = 26;

    private final GuiServices services;
    private final UUID targetId;
    private final String targetName;
    private final Map<Integer, String> slotToClassId = new LinkedHashMap<>();

    public AdminClassPickerMenu(GuiServices services, UUID targetId, String targetName) {
        this.services = services;
        this.targetId = targetId;
        this.targetName = targetName;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_CLASS_PICKER_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(CLEAR_SLOT, GuiItems.of(Material.BARRIER, services.messages().render(MessageKey.GUI_ADMIN_CLASS_PICKER_CLEAR_ITEM)));
        int slot = FIRST_CLASS_SLOT;
        for (PlayerClassDefinition definition : services.classes().definitions()) {
            slotToClassId.put(slot, definition.id());
            inventory.setItem(slot++, GuiItems.of(definition.icon(),
                    services.messages().render(MessageKey.GUI_ADMIN_CLASS_PICKER_ITEM, Map.of("sinif", definition.displayName()))));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == BACK_SLOT) {
            GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName));
            return;
        }
        String classId = slot == CLEAR_SLOT ? "" : slotToClassId.get(slot);
        if (classId == null) {
            return;
        }
        services.classes().adminForceSetClass(targetId, targetName, classId).thenRun(() -> {
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun sinifini "
                    + (classId.isEmpty() ? "temizledi" : classId + " olarak zorla ayarladi") + ".");
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }
}
