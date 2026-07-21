package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.ClassSelectionResult;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * "Are you sure?" confirmation for one class pick from {@link ClassIntroMenu}
 * (or reached again later if a player somehow re-enters this flow with an
 * existing class already selected - the wording branches accordingly).
 */
public final class ClassConfirmMenu implements GuiView {

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final GuiServices services;
    private final String classId;

    public ClassConfirmMenu(GuiServices services, String classId) {
        this.services = services;
        this.classId = classId;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_CLASS_CONFIRM_TITLE);
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        services.profiles().load(player.getUniqueId(), player.getName())
                .thenAccept(profile -> player.getScheduler().run(services.plugin(), task -> render(inventory, profile), null));
    }

    private void render(Inventory inventory, PlayerProfile profile) {
        Component notice = profile.selectedClass().isEmpty()
                ? services.messages().render(MessageKey.GUI_CLASS_CONFIRM_FIRST_PICK_NOTICE)
                : services.messages().render(MessageKey.GUI_CLASS_CONFIRM_COOLDOWN_WARNING, Map.of(
                        "gun", Long.toString(services.classes().settings().changeCooldownMillis() / 86_400_000L)
                ));
        inventory.setItem(CONFIRM_SLOT, GuiItems.of(
                Material.LIME_DYE,
                services.messages().render(MessageKey.GUI_CLASS_CONFIRM_BUTTON),
                List.of(notice)
        ));
        inventory.setItem(CANCEL_SLOT, GuiItems.of(Material.RED_DYE, services.messages().render(MessageKey.GUI_CLASS_CONFIRM_CANCEL_BUTTON)));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == CANCEL_SLOT) {
            GuiManager.open(player, new ClassIntroMenu(services));
            return;
        }
        if (slot != CONFIRM_SLOT) {
            return;
        }
        services.classes().select(player.getUniqueId(), player.getName(), classId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> {
                    if (result.status() == ClassSelectionResult.Status.SELECTED) {
                        services.classes().applyPlayerEffects(player);
                        services.energy().reconcile(player);
                        services.abilities().clearSelection(player.getUniqueId());
                        services.sidebar().refresh(player);
                        services.feedback().celebrateClassSelected(player);
                        services.messages().send(player, MessageKey.CLASS_SELECTED, Map.of("sinif", services.classes().displayName(result.classId())));
                        player.closeInventory();
                        return;
                    }
                    switch (result.status()) {
                        case ALREADY_SELECTED -> services.messages().send(player, MessageKey.CLASS_ALREADY_SELECTED,
                                Map.of("sinif", services.classes().displayName(result.classId())));
                        case UNKNOWN_CLASS -> services.messages().send(player, MessageKey.CLASS_UNKNOWN,
                                Map.of("sinif", result.classId()));
                        case COOLDOWN -> services.messages().send(player, MessageKey.CLASS_COOLDOWN,
                                Map.of("sure", Long.toString(Math.max(1, (result.remainingMillis() + 999) / 1000))));
                        case SELECTED -> throw new IllegalStateException();
                    }
                    player.closeInventory();
                }, null));
    }
}
