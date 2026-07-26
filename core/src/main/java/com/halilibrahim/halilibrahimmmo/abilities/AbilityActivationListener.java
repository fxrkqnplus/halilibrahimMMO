package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /** How long one right click owns the activation path, across every event it may raise. */
    private static final long ACTIVATION_DEBOUNCE_MILLIS = 200L;

    private final AbilityService abilities;
    private final EnergyService energy;
    private final ConcurrentHashMap<UUID, Long> lastActivationAttempt = new ConcurrentHashMap<>();

    public AbilityActivationListener(AbilityService abilities, EnergyService energy) {
        this.abilities = abilities;
        this.energy = energy;
    }

    /**
     * Deliberately <em>not</em> {@code ignoreCancelled}: a right click on empty
     * air arrives with {@code useInteractedBlock} already denied (there is no
     * block to interact with), which makes {@link PlayerInteractEvent#isCancelled()}
     * report {@code true} and made the whole air-click path invisible to this
     * listener. Only block clicks ever reached activation as a result, so an
     * attack ability could not be armed without first finding a wall to click.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        tryActivate(event.getPlayer());
    }

    /**
     * Right-clicking a creature raises {@link PlayerInteractEntityEvent} instead
     * of {@link PlayerInteractEvent}, so without this handler an ability could
     * never be started while aiming at the very mobs the combat abilities exist
     * to fight.
     */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        tryActivate(event.getPlayer());
    }

    private void tryActivate(Player player) {
        if (!claimActivationWindow(player)) {
            return;
        }
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

    /**
     * A single right click can raise both handled events (an entity click also
     * reports as an air click on some interactions), which would otherwise
     * activate once and then immediately reject the duplicate with a cooldown
     * message the player never triggered. One activation per click window wins.
     */
    private boolean claimActivationWindow(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastActivationAttempt.put(player.getUniqueId(), now);
        return previous == null || now - previous >= ACTIVATION_DEBOUNCE_MILLIS;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        abilities.clearActivationFeedback(event.getPlayer());
        lastActivationAttempt.remove(event.getPlayer().getUniqueId());
    }
}
