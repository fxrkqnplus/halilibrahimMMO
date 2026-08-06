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
        GuiItems.frame(inventory, GuiItems.pane(Material.ORANGE_STAINED_GLASS_PANE), GuiItems.filler());
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
        services.classes().adminForceSetClass(targetId, targetName, classId).thenAccept(previousClassId -> {
            // The same unequip the player's own change performs: an ability
            // selected for the class the target no longer has must not stay
            // equipped just because staff made the change instead of them.
            services.abilities().clearSelection(targetId);
            // ...and the same wipe of the abandoned class's level track. This
            // used to be the one difference between the two paths, and it was
            // not a defensible one: the class a player is no longer on starts
            // over, and who pressed the button does not change that.
            if (!previousClassId.isEmpty() && !previousClassId.equals(classId)) {
                services.classes().resetClassLevel(targetId, targetName, previousClassId);
            }
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun sinifini "
                    + (classId.isEmpty() ? "temizledi" : classId + " olarak zorla ayarladi") + ".");
            Player target = services.plugin().getServer().getPlayer(targetId);
            if (target != null) {
                target.getScheduler().run(services.plugin(), task -> announceToTarget(target, classId), null);
            }
            player.getScheduler().run(services.plugin(), task -> GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName)), null);
        });
    }

    /**
     * Everything the target's own confirmation screen does to them, on the
     * target's own scheduler.
     *
     * <p>The attribute modifiers and the energy ceiling are not cosmetic: a
     * player left on the old class's maximum health after being moved to a new
     * one is simply wrong until their next login. Clearing the class is the one
     * case with nothing to announce - there is no new class to name - but the
     * effects still have to be reconciled down.</p>
     */
    private void announceToTarget(Player target, String classId) {
        services.classes().applyPlayerEffects(target);
        services.energy().reconcile(target);
        services.sidebar().refresh(target);
        if (!classId.isEmpty()) {
            services.feedback().celebrateClassChangedByAdmin(target, services.classes().displayName(classId));
        }
    }
}
