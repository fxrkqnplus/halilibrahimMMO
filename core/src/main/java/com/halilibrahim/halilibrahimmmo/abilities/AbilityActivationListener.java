package com.halilibrahim.halilibrahimmmo.abilities;

import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
 * <p><strong>Four things never reach the confirm step at all.</strong> A right
 * click on a block that already does something of its own is ignored, a right
 * click that is really the held item being used - placing a block, emptying a
 * bucket, putting down a boat - is ignored (both via
 * {@link InteractableBlocks}), and outside Creative a click aimed at an ability
 * that is already running, or still on cooldown, never prompts either: the
 * second click could only ever be refused, so asking for it was asking the
 * player to confirm a refusal. Running says nothing (the countdown is already
 * on screen); on cooldown puts the remaining wait on the action bar.</p>
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
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isBlockInteraction(event)) {
            return;
        }
        tryActivate(event.getPlayer());
    }

    /**
     * The two ways a right click on a block is about the world rather than
     * about an ability.
     *
     * <p><strong>The block already does something.</strong> A door, a lever, a
     * furnace - see {@link InteractableBlocks#isInteractable}. Sneaking is the
     * escape hatch here, because vanilla already suppresses the block's own
     * behaviour while sneaking, so the gesture a player knows for "ignore this
     * block" is the one that gets their ability at a door.</p>
     *
     * <p><strong>The held item is being used.</strong> Placing a block is the
     * single most common right click in the game, and for an ability whose tool
     * is {@code ANY} there is nothing else to rule it out - laying a wall was
     * spending Sürü Çağrısı once every couple of blocks. Sneaking deliberately
     * does <em>not</em> lift this one: sneaking is how a block gets placed
     * against a chest, so honouring it would reopen the hole exactly where it
     * hurts. Right-clicking open air still activates normally, since nothing is
     * placed there.</p>
     */
    private boolean isBlockInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (InteractableBlocks.isUseItem(player.getInventory().getItemInMainHand().getType())) {
            return true;
        }
        return !player.isSneaking()
                && event.getClickedBlock() != null
                && InteractableBlocks.isInteractable(event.getClickedBlock().getType());
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
        boolean alreadyRunning = abilities.activeStatus(player.getUniqueId())
                .filter(status -> status.abilityId().equals(definition.id()))
                .isPresent();
        boolean creative = player.getGameMode() == GameMode.CREATIVE;
        // Outside Creative there is nothing a second activation could do while
        // the first is still running: it would only ever be answered with a
        // cooldown refusal. Saying nothing at all is the honest response - the
        // action-bar countdown is already telling the player it is running, and
        // every stray right click was otherwise prompting and then refusing.
        if (alreadyRunning && !creative) {
            return;
        }
        // Same reasoning one step further along: an ability still on cooldown
        // cannot be activated by a second click either, so prompting for one
        // was asking the player to confirm a refusal. The remaining wait goes
        // on the action bar instead - the same surface the prompt used, so it
        // costs no chat noise however often the click is repeated.
        if (!creative && !alreadyRunning) {
            long cooldownRemaining = abilities.cooldownRemainingMillis(player.getUniqueId(), definition.id());
            if (cooldownRemaining > 0) {
                player.sendActionBar(messages.render(MessageKey.ABILITY_COOLDOWN_ACTIONBAR, Map.of(
                        "kabiliyet", definition.displayName(),
                        "sure", DurationFormatter.ofMillis(messages, cooldownRemaining)
                )));
                return;
            }
        }
        boolean deactivating = creative && alreadyRunning;
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
        // A pickaxe Madenci Refleksleri lent three Efficiency levels to must not
        // leave the server still carrying them.
        abilities.restoreEfficiency(event.getPlayer());
        // A wolf that logs out has to stop being one on everybody else's screen.
        abilities.handlePlayerQuit(event.getPlayer());
        lastActivationAttempt.remove(event.getPlayer().getUniqueId());
        pendingActivations.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Dying ends the ability outright.
     *
     * <p>Every visible half of an ability was already gone at this point -
     * potion effects are wiped by the death, the tool is on the ground - but the
     * plugin's own state was not, so a player who died mid-ability respawned
     * with the buff still armed and the countdown still running against a fight
     * they had lost. {@code MONITOR}: this is bookkeeping after the fact, and it
     * must not depend on anything else's decision about the death.</p>
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        abilities.endOnDeath(event.getEntity());
        pendingActivations.remove(event.getEntity().getUniqueId());
    }

    /**
     * The other half of the same guarantee: a pickaxe that <em>did</em> get away
     * still enchanted - because the server stopped mid-ability, or because it
     * was stashed in a chest and only came back later - is put right the moment
     * it is next carried onto the server. The original level rides on the item,
     * so whoever is holding it is enough.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        abilities.restoreEfficiency(event.getPlayer());
        // The same guarantee for the wolf's claws: a server that went down
        // mid-transformation left somebody's hotbar in their own persistent
        // data, and this is where it comes back.
        abilities.restoreHotbarIfStranded(event.getPlayer());
        // The wolf-form set is only broadcast when it changes, so somebody
        // joining mid-transformation has to be told the current truth once.
        abilities.sendWolfForms(event.getPlayer());
    }

    /**
     * Kan Yemini's soul cue. Lives here rather than in {@code SkillListener}
     * because it is ability feedback, not skill progression - and unlike skill
     * XP it must fire whether or not the kill was worth anything.
     */
    @EventHandler
    public void onCreatureDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player)) {
            abilities.handleCreatureDeath(killer, event.getEntity().getLocation(), event.getEntity().getType());
        }
    }

    /**
     * Asil Kurt's leap costs nothing on the way down.
     *
     * <p>{@code HIGHEST} rather than {@code MONITOR}: this cancels the event, and
     * a cancellation has to happen before anything that only observes the
     * outcome. Left cancellable-aware, so a protection plugin that already
     * decided this fall does not hurt keeps its decision.</p>
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        abilities.handleFallDamage(event);
    }

    /**
     * Sneaking in mid-air is Asil Kurt throwing itself at the ground.
     *
     * <p>Never cancelled: the sneak itself is the player's, and the ability
     * reads it rather than consuming it. Every non-wolf, non-airborne case is
     * turned away inside the service, which is the only place that knows who is
     * transformed.</p>
     */
    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        abilities.handleSneak(event.getPlayer(), event.isSneaking());
    }

    /**
     * The claws stay in the hand. Asil Kurt takes the hotbar away and gives back
     * one item, and these four handlers are what make that stick: no switching
     * slots, no dropping, no swapping to the off hand, no moving anything in any
     * inventory, and nothing picked up off the floor.
     *
     * <p><strong>Refused rather than silently ignored</strong>, and refused with
     * one message per gesture rather than per attempt - a player mashing a number
     * key should learn the rule once, not sixty times. The action bar is the
     * surface for exactly this reason: it overwrites itself.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!abilities.isTransformed(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendActionBar(messages.render(MessageKey.ABILITY_NOBLE_WOLF_LOCKED_SLOT));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        if (!abilities.isTransformed(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendActionBar(messages.render(MessageKey.ABILITY_NOBLE_WOLF_LOCKED_ITEMS));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (abilities.isTransformed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !abilities.isTransformed(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        player.sendActionBar(messages.render(MessageKey.ABILITY_NOBLE_WOLF_LOCKED_ITEMS));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && abilities.isTransformed(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Nothing is picked up while the claws are on.
     *
     * <p>The drop stays where it fell rather than being destroyed, and vanilla
     * leaves it there for five minutes - far longer than the transformation
     * lasts - so this costs a player nothing but a walk back. It is also the only
     * version of "no other items" that does not quietly refill the hotbar the
     * ability just emptied.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        if (abilities.isTransformed(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private record PendingActivation(String abilityId, boolean deactivating, long expiresAtMillis) {

        boolean matches(String otherAbilityId, boolean otherDeactivating) {
            return abilityId.equals(otherAbilityId) && deactivating == otherDeactivating;
        }
    }
}
