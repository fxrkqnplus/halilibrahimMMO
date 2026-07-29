package com.halilibrahim.halilibrahimmmo.gui;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lets staff equip an ability on a player's behalf, or take the one they have
 * off.
 *
 * <p>Deliberately routed through the ordinary {@code AbilityService#select}
 * and {@code #unequip} calls the player's own screen uses, rather than writing
 * the selection somewhere directly: everything those methods do around the
 * selection - ending what the previous one left running, the confirmation
 * message, the sound cue, the switch-cooldown bookkeeping - is exactly what
 * should happen when an admin does it too. The one thing this screen adds is
 * that the class gate is checked here rather than being discovered on
 * activation, so a locked ability is simply not offered.</p>
 *
 * <p><strong>Online targets only.</strong> The selection lives in memory and is
 * cleared on quit, so setting it for an offline player would be writing to
 * something that no longer exists by the time they return.</p>
 */
public final class AdminAbilityPickerMenu implements GuiView {

    private static final int HEADER_SLOT = 4;
    private static final int[] ABILITY_SLOTS = {19, 21, 23, 25};
    private static final int UNEQUIP_SLOT = 40;
    private static final int BACK_SLOT = 36;

    private final GuiServices services;
    private final UUID targetId;
    private final String targetName;
    private final boolean fromOnlineList;
    private final Map<Integer, String> slotToAbilityId = new LinkedHashMap<>();

    public AdminAbilityPickerMenu(GuiServices services, UUID targetId, String targetName, boolean fromOnlineList) {
        this.services = services;
        this.targetId = targetId;
        this.targetName = targetName;
        this.fromOnlineList = fromOnlineList;
    }

    @Override
    public Component title() {
        return services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_TITLE);
    }

    @Override
    public int size() {
        return 45;
    }

    @Override
    public void populate(Player player, Inventory inventory) {
        GuiItems.frame(inventory, GuiItems.pane(Material.ORANGE_STAINED_GLASS_PANE), GuiItems.filler());
        inventory.setItem(BACK_SLOT, GuiItems.backButton(services.messages()));
        inventory.setItem(HEADER_SLOT, GuiItems.playerHead(Bukkit.getOfflinePlayer(targetId),
                services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_HEADER, Map.of("oyuncu", targetName)),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_HEADER_LORE))));
        inventory.setItem(UNEQUIP_SLOT, GuiItems.of(Material.BARRIER,
                services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_CLEAR_ITEM),
                List.of(services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_CLEAR_LORE))));

        String classId = services.profiles().cached(targetId).map(profile -> profile.selectedClass()).orElse("");
        if (classId.isEmpty()) {
            inventory.setItem(ABILITY_SLOTS[0], GuiItems.of(Material.BARRIER,
                    services.messages().render(MessageKey.GUI_ADMIN_EDIT_NO_CLASS_ITEM)));
            return;
        }
        int classLevel = services.classes().classProgress(targetId, classId).level();
        String equipped = services.abilities().selected(targetId).orElse("");
        List<AbilityDefinition> abilities = services.abilities().definitions().stream()
                .filter(definition -> definition.classId().equals(classId))
                .sorted((left, right) -> Integer.compare(left.requiredLevel(), right.requiredLevel()))
                .toList();
        for (int index = 0; index < abilities.size() && index < ABILITY_SLOTS.length; index++) {
            AbilityDefinition definition = abilities.get(index);
            int slot = ABILITY_SLOTS[index];
            boolean unlocked = classLevel >= definition.requiredLevel();
            if (!unlocked) {
                inventory.setItem(slot, GuiItems.of(Material.BARRIER,
                        services.messages().render(MessageKey.GUI_ABILITY_LOCKED_ITEM),
                        List.of(services.messages().render(MessageKey.GUI_ABILITY_LOCKED_LORE,
                                Map.of("seviye", Integer.toString(definition.requiredLevel()))))));
                continue;
            }
            slotToAbilityId.put(slot, definition.id());
            List<Component> lore = new ArrayList<>();
            lore.add(services.messages().renderText(definition.description()));
            lore.add(services.messages().render(MessageKey.GUI_ADMIN_ABILITY_PICKER_SELECT_HINT));
            inventory.setItem(slot, GuiItems.of(definition.icon(),
                    services.messages().render(MessageKey.GUI_ABILITY_ITEM_LORE_DESCRIPTION,
                            Map.of("kabiliyet", definition.displayName())),
                    lore, definition.id().equals(equipped)));
        }
    }

    @Override
    public void handleClick(Player player, int slot, ItemStack clicked, ClickType click) {
        Player target = Bukkit.getPlayer(targetId);
        if (slot == BACK_SLOT || target == null) {
            GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName, fromOnlineList));
            return;
        }
        if (slot == UNEQUIP_SLOT) {
            target.getScheduler().run(services.plugin(), task -> services.abilities().unequip(target), null);
            services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusunun kabiliyetini cikardi.");
            GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName, fromOnlineList));
            return;
        }
        String abilityId = slotToAbilityId.get(slot);
        if (abilityId == null) {
            return;
        }
        // On the target's own region, because select(...) touches their held
        // item's glint and sends them a message.
        target.getScheduler().run(services.plugin(), task -> services.abilities().select(target, abilityId), null);
        services.logger().info("Yonetici " + player.getName() + ", " + targetName + " oyuncusuna "
                + abilityId + " kabiliyetini kusandirdi.");
        GuiManager.open(player, new AdminPlayerEditMenu(services, targetId, targetName, fromOnlineList));
    }
}
