package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Right-click activation trigger for the menu-selected ability
 * ({@link AbilityService#select}).
 *
 * <p><strong>Activation takes two right clicks, not one.</strong> The first
 * click arms nothing and only puts a prompt on the action bar; a second click
 * within {@link #CONFIRM_WINDOW_MILLIS} is what actually fires the ability.
 * Abilities cost energy and start long cooldowns, and a single right click is
 * something a player does constantly by accident - opening a door, placing a
 * block, feeding an animal - so a one-click trigger spent those resources on
 * moments the player never intended. The confirm window is short enough that a
 * deliberate double click feels immediate.</p>
 *
 * <p><strong>In Creative the second click also turns a running ability back
 * off.</strong> There, abilities are a thing to be tried out rather than
 * budgeted, so the prompt and the toggle read as one interaction: click twice
 * to start it, click twice to stop it.</p>
 *
 * <p>Switching or dropping the held item deliberately does nothing here. A tool
 * is only required to <em>start</em> an ability; once running it stays running
 * for its full duration regardless of what the player holds. The only remaining
 * cleanup point is leaving the server.</p>
 */
public final class AbilityActivationListener implements Listener {

    /** How long one right click owns the activation path, across every event it may raise. */
    private static final long ACTIVATION_DEBOUNCE_MILLIS = 200L;
    /** How long the "click again" prompt stands before the first click is forgotten. */
    private static final long CONFIRM_WINDOW_MILLIS = 3_000L;

    private final AbilityService abilities;
    private final EnergyService energy;
    private final MessageService messages;
    private final ConcurrentHashMap<UUID, Long> lastActivationAttempt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingActivation> pendingActivations = new ConcurrentHashMap<>();

    public AbilityActivationListener(AbilityService abilities, EnergyService energy, MessageService messages) {
        this.abilities = abilities;
        this.energy = energy;
        this.messages = messages;
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
        boolean deactivating = player.getGameMode() == GameMode.CREATIVE
                && abilities.activeStatus(player.getUniqueId())
                        .filter(status -> status.abilityId().equals(definition.id()))
                        .isPresent();
        if (!claimConfirmation(player, definition, deactivating)) {
            return;
        }
        if (deactivating) {
            abilities.deactivate(player);
            return;
        }
        abilities.activate(player, definition.id(), energy);
    }

    /**
     * The two-click gate. The first click stores what it was aiming at and
     * prompts; only a second click at the same ability, for the same purpose,
     * within the window gets through. Storing the intent (rather than a bare
     * timestamp) is what stops a click that armed one ability from confirming a
     * different one the player switched to in between.
     */
    private boolean claimConfirmation(Player player, AbilityDefinition definition, boolean deactivating) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        PendingActivation pending = pendingActivations.get(playerId);
        if (pending != null && pending.matches(definition.id(), deactivating) && now <= pending.expiresAtMillis()) {
            pendingActivations.remove(playerId);
            return true;
        }
        pendingActivations.put(playerId, new PendingActivation(definition.id(), deactivating, now + CONFIRM_WINDOW_MILLIS));
        player.sendActionBar(messages.render(
                deactivating ? MessageKey.ABILITY_CONFIRM_DEACTIVATE : MessageKey.ABILITY_CONFIRM_ACTIVATE,
                Map.of("kabiliyet", definition.displayName())
        ));
        return false;
    }

    /**
     * A single right click can raise both handled events (an entity click also
     * reports as an air click on some interactions), which would otherwise
     * consume the confirmation prompt with the very click that raised it.
     * One activation attempt per click window wins.
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
        pendingActivations.remove(event.getPlayer().getUniqueId());
    }

    private record PendingActivation(String abilityId, boolean deactivating, long expiresAtMillis) {

        boolean matches(String otherAbilityId, boolean otherDeactivating) {
            return abilityId.equals(otherAbilityId) && deactivating == otherDeactivating;
        }
    }
}
