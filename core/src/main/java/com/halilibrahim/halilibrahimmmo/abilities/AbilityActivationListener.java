package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
 * <p><strong>Switching or dropping the held item deliberately does nothing
 * here.</strong> Earlier phases cleared the activation feedback (glint,
 * action-bar countdown/energy readout, ability-end sound) whenever the held
 * item stopped matching the running ability's tool, which made an ability look
 * and sound like it had broken the moment a player reached for anything else -
 * even though the underlying armed/buff state was still perfectly alive and
 * still expired or triggered on schedule. A tool is only required to
 * <em>start</em> an ability; once running, it stays running for its full
 * duration regardless of what the player holds. The only remaining cleanup
 * point is leaving the server; every other teardown is driven by the ability
 * itself (expiry, consumption, energy depletion) inside
 * {@code YamlAbilityService}.</p>
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
    public void onQuit(PlayerQuitEvent event) {
        abilities.clearActivationFeedback(event.getPlayer());
    }
}
