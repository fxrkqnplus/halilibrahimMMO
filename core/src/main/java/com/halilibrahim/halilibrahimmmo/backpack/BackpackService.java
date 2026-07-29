package com.halilibrahim.halilibrahimmmo.backpack;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The cloud chest: one single-chest worth of storage per player that lives in
 * the database rather than in the world.
 *
 * <p>Because the items are never in the player's own inventory and never in a
 * block anybody can find, <strong>death cannot touch them</strong> - which is
 * the whole point of the feature and the reason it is gated behind a class
 * level rather than handed out at the start. It can be opened in any biome, any
 * dimension, any world, since there is nothing physical for it to be near.</p>
 *
 * <p>This is the one place in the codebase where an inventory the plugin opens
 * really is storage. Every other menu is display-only and is pre-cancelled by
 * {@code GuiListener} - see {@code BackpackHolder} for how the two are kept
 * from being confused for each other.</p>
 */
public interface BackpackService extends PluginService {

    /** How many slots a backpack has: exactly one single chest. */
    int SIZE = 27;

    /** Whether this player has reached the level (or the prestige) that unlocks it. */
    boolean isUnlocked(UUID playerId);

    /**
     * Builds this player's backpack inventory from storage.
     *
     * <p>Asynchronous because the contents live in the database and are not
     * cached between opens: a backpack is read at most a few times a session,
     * and a stale copy in memory is exactly the wrong trade for something whose
     * whole promise is that what you put in is what you get back.</p>
     */
    CompletableFuture<Inventory> open(Player player);

    /** Writes an open backpack back to storage - called when its window closes. */
    CompletableFuture<Void> save(UUID playerId, Inventory inventory);
}
