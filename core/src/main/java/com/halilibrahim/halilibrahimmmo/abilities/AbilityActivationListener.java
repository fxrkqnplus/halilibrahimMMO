package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Right-click activation trigger for the menu-selected ability
 * ({@link AbilityService#select}): a right click while holding a tool that
 * matches the selected ability's {@link AbilityToolTag} activates it through
 * the exact same {@link AbilityService#activate} path {@code /kabiliyet}
 * already uses, so level/energy/cooldown checks stay identical either way.
 *
 * <p>Also clears activation visual feedback (glint/action-bar countdown) on
 * the handful of interactions that would otherwise leave it stale - dropping
 * an item, switching hotbar slots, or leaving. This never touches the
 * underlying armed/buff game state, which keeps expiring or triggering
 * exactly as it always has.</p>
 */
public final class AbilityActivationListener implements Listener {

    private final AbilityService abilities;
    private final EnergyService energy;

    public AbilityActivationListener(AbilityService abilities, EnergyService energy) {
        this.abilities = abilities;
        this.energy = energy;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        Player player = event.getPlayer();
        String selectedId = abilities.selected(player.getUniqueId()).orElse(null);
        if (selectedId == null) {
            return;
        }
        AbilityDefinition definition = abilities.definitions().stream()
                .filter(candidate -> candidate.id().equals(selectedId))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        boolean toolOk = definition.tool() == AbilityToolTag.ANY
                || (!item.getType().isAir() && definition.tool().matches(item.getType()));
        if (!toolOk) {
            return;
        }
        abilities.activate(player, definition.id(), energy);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        abilities.clearActivationFeedback(event.getPlayer());
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        abilities.clearActivationFeedback(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        abilities.clearActivationFeedback(event.getPlayer());
    }
}
