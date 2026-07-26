package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.classes.ClassSelectionResult;
import com.halilibrahim.halilibrahimmmo.classes.PlayerClassProgress;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * "Are you sure?" confirmation for one class pick from {@link ClassIntroMenu}.
 *
 * <p>Everything irreversible about the choice is spelled out here before the
 * button is ever clickable: how long the next change will be locked out, and -
 * the part a player is most likely to be surprised by - that the class they are
 * leaving has its class level wiped back to 1. A player who is still inside the
 * change cooldown never sees a confirm button at all; the slot shows the
 * remaining wait instead, so the rejection is visible up front rather than
 * arriving as an error after clicking.</p>
 */
public final class ClassConfirmMenu implements GuiView {

    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;

    private final GuiServices services;
    private final String classId;
    private final boolean fromMenu;
    private volatile boolean confirmable;

    public ClassConfirmMenu(GuiServices services, String classId, boolean fromMenu) {
        this.services = services;
        this.classId = classId;
        this.fromMenu = fromMenu;
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
                .thenCompose(profile -> services.classes().load(player.getUniqueId(), player.getName())
                        .thenApply(progress -> new Object[]{profile, progress}))
                .thenAccept(values -> player.getScheduler().run(services.plugin(), task ->
                        render(inventory, (PlayerProfile) values[0], (PlayerClassProgress) values[1]), null));
    }

    private void render(Inventory inventory, PlayerProfile profile, PlayerClassProgress progress) {
        inventory.setItem(CANCEL_SLOT, GuiItems.of(Material.RED_DYE, services.messages().render(MessageKey.GUI_CLASS_CONFIRM_CANCEL_BUTTON)));

        String currentClassId = profile.selectedClass();
        long remaining = progress.classChangeReadyAtMillis() - System.currentTimeMillis();
        if (!currentClassId.isEmpty() && remaining > 0) {
            confirmable = false;
            inventory.setItem(CONFIRM_SLOT, GuiItems.of(
                    Material.BARRIER,
                    services.messages().render(MessageKey.GUI_CLASS_CONFIRM_LOCKED_ITEM),
                    List.of(services.messages().render(MessageKey.GUI_CLASS_CONFIRM_LOCKED_LORE, Map.of(
                            "sure", DurationFormatter.ofMillis(services.messages(), remaining)
                    )))
            ));
            return;
        }

        confirmable = true;
        List<Component> lore = new ArrayList<>();
        lore.add(services.messages().render(MessageKey.GUI_CLASS_CONFIRM_TARGET, Map.of("sinif", services.classes().displayName(classId))));
        if (currentClassId.isEmpty()) {
            lore.add(services.messages().render(MessageKey.GUI_CLASS_CONFIRM_FIRST_PICK_NOTICE));
        } else {
            lore.add(services.messages().render(MessageKey.GUI_CLASS_CONFIRM_RESET_WARNING, Map.of(
                    "sinif", services.classes().displayName(currentClassId)
            )));
            lore.add(services.messages().render(MessageKey.GUI_CLASS_CONFIRM_COOLDOWN_WARNING, Map.of(
                    "sure", DurationFormatter.ofMillis(services.messages(), services.classes().settings().changeCooldownMillis())
            )));
        }
        inventory.setItem(CONFIRM_SLOT, GuiItems.of(Material.LIME_DYE,
                services.messages().render(MessageKey.GUI_CLASS_CONFIRM_BUTTON), lore));
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        if (slot == CANCEL_SLOT) {
            GuiManager.open(player, new ClassIntroMenu(services, fromMenu));
            return;
        }
        if (slot != CONFIRM_SLOT || !confirmable) {
            return;
        }
        String previousClassId = services.profiles().cached(player.getUniqueId())
                .map(PlayerProfile::selectedClass).orElse("");
        services.classes().select(player.getUniqueId(), player.getName(), classId).thenAccept(result ->
                player.getScheduler().run(services.plugin(), task -> apply(player, result, previousClassId), null));
    }

    private void apply(Player player, ClassSelectionResult result, String previousClassId) {
        if (result.status() != ClassSelectionResult.Status.SELECTED) {
            switch (result.status()) {
                case ALREADY_SELECTED -> services.messages().send(player, MessageKey.CLASS_ALREADY_SELECTED,
                        Map.of("sinif", services.classes().displayName(result.classId())));
                case UNKNOWN_CLASS -> services.messages().send(player, MessageKey.CLASS_UNKNOWN,
                        Map.of("sinif", result.classId()));
                case COOLDOWN -> services.messages().send(player, MessageKey.CLASS_COOLDOWN,
                        Map.of("sure", DurationFormatter.ofMillis(services.messages(), result.remainingMillis())));
                case SELECTED -> throw new IllegalStateException();
            }
            player.closeInventory();
            return;
        }
        services.classes().applyPlayerEffects(player);
        services.energy().reconcile(player);
        services.abilities().clearSelection(player.getUniqueId());
        services.sidebar().refresh(player);
        if (previousClassId.isEmpty()) {
            services.feedback().celebrateClassSelected(player);
            services.messages().send(player, MessageKey.CLASS_SELECTED, Map.of("sinif", services.classes().displayName(result.classId())));
            player.closeInventory();
            return;
        }
        // The abandoned class starts over: its level track is wiped here rather
        // than inside ClassService#select, so the reset only ever happens on the
        // explicitly-confirmed change path and never on an admin force-set.
        services.classes().resetClassLevel(player.getUniqueId(), player.getName(), previousClassId)
                .thenRun(() -> player.getScheduler().run(services.plugin(), task -> services.sidebar().refresh(player), null));
        services.feedback().celebrateClassChanged(player, services.classes().displayName(result.classId()));
        player.closeInventory();
    }
}
