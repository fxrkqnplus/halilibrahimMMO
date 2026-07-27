package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.gui.GuiHolder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the two exploration-flavoured quest objectives:
 * {@link QuestObjectiveType#STRUCTURE_DISCOVER} and
 * {@link QuestObjectiveType#ITEM_OBTAIN}.
 *
 * <h2>Structures</h2>
 * <p>There is no "player entered a structure" event, so this sweeps online
 * players on a timer and asks the world which generated structures overlap the
 * chunk they are standing in ({@code World#getStructures(chunkX, chunkZ)}),
 * then tests each one's bounding box. The sweep is why this listener owns a
 * {@link ScheduledTask} - the same reason {@code HusbandryListener} owns one
 * for its lead-carrying tick, and it is started and stopped explicitly by the
 * plugin rather than turning a listener into a {@code PluginService}.</p>
 *
 * <p>A player standing in a mineshaft for ten minutes must not tick a quest
 * ten thousand times, so each (player, structure) pair is remembered for the
 * session and only credited on the sweep that first finds them inside it.
 * These quests all have a target of one anyway; the memo is what keeps the
 * cost at one future per genuinely new discovery.</p>
 *
 * <h2>Items</h2>
 * <p>Both ways an item actually reaches a player are covered: picking it up off
 * the ground, and taking it out of a container. The container half deliberately
 * ignores this plugin's own menus - their contents are display icons, and a
 * click on one is cancelled before it moves anything, so crediting a quest for
 * it would hand out a music disc for opening the quest board.</p>
 */
public final class QuestDiscoveryListener implements Listener {

    /** How often online players are checked against the structures around them. */
    private static final long STRUCTURE_SWEEP_TICKS = 40L;

    /** Container clicks that genuinely move an item into the player's possession. */
    private static final Set<InventoryAction> TAKING_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.HOTBAR_SWAP
    );

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final ActivationService activation;
    private final QuestCompletionHandler completions;
    private final ConcurrentHashMap<UUID, Set<Structure>> discovered = new ConcurrentHashMap<>();
    private ScheduledTask sweepTask;

    public QuestDiscoveryListener(
            JavaPlugin plugin, QuestService quests, ActivationService activation, QuestCompletionHandler completions
    ) {
        this.plugin = plugin;
        this.quests = quests;
        this.activation = activation;
        this.completions = completions;
    }

    public void start() {
        sweepTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task ->
                Bukkit.getOnlinePlayers().forEach(player ->
                        player.getScheduler().run(plugin, ignored -> checkStructures(player), null)),
                STRUCTURE_SWEEP_TICKS, STRUCTURE_SWEEP_TICKS);
    }

    public void close() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        discovered.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        discovered.remove(event.getPlayer().getUniqueId());
    }

    /** Runs on the player's own region scheduler, so reading their world and chunk here is safe. */
    private void checkStructures(Player player) {
        if (!activation.cached(player.getUniqueId()) || !isGameplayGameMode(player)) {
            return;
        }
        Location location = player.getLocation();
        for (GeneratedStructure generated : player.getWorld().getStructures(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            if (!generated.getBoundingBox().contains(location.toVector())) {
                continue;
            }
            Structure structure = generated.getStructure();
            Set<Structure> seen = discovered.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
            if (!seen.add(structure)) {
                continue;
            }
            quests.awardStructure(player.getUniqueId(), structure)
                    .thenAccept(completed -> completions.notifyCompleted(player, completed));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        award(player, event.getItem().getItemStack().getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void onContainerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !TAKING_ACTIONS.contains(event.getAction())) {
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof GuiHolder) {
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked.equals(player.getInventory())) {
            return;
        }
        ItemStack taken = event.getCurrentItem();
        if (taken != null) {
            award(player, taken.getType());
        }
    }

    private void award(Player player, Material material) {
        if (!quests.tracksItem(material) || !activation.cached(player.getUniqueId()) || !isGameplayGameMode(player)) {
            return;
        }
        quests.awardItem(player.getUniqueId(), material)
                .thenAccept(completed -> completions.notifyCompleted(player, completed));
    }

    private boolean isGameplayGameMode(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }
}
