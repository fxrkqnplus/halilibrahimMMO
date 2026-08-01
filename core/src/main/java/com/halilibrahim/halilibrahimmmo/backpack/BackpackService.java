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

    /**
     * How many slots a brand-new backpack has.
     *
     * <p>Two rows, deliberately not a full chest. A backpack handed out at its
     * final size is a reward that stops meaning anything the moment it unlocks;
     * two rows are enough to be genuinely useful and short enough that the first
     * widening is something a player wants.</p>
     */
    int BASE_SIZE = 18;

    /** What one widening buys. */
    int SLOTS_PER_UPGRADE = 7;

    /** How many times it can be widened, ever. Two, so the ceiling is 32 slots. */
    int MAXIMUM_UPGRADES = 2;

    /**
     * The inventory the window is actually built at: the next multiple of nine
     * at or above {@code capacity}.
     *
     * <p>Minecraft has no 25-slot chest - a chest inventory is a multiple of
     * nine or it does not open at all - so the window is built one row larger
     * than it needs to be and the slots past the capacity are filled with a
     * blocked pane. Buying seven slots and being handed nine would be a
     * pleasant surprise exactly once and a confusing number forever after.
     */
    static int windowSize(int capacity) {
        return (capacity + 8) / 9 * 9;
    }

    /** Whether this player has reached the level (or the prestige) that unlocks it. */
    boolean isUnlocked(UUID playerId);

    /** How many wideings this player has bought. Synchronous, join-warmed; {@code 0} until loaded. */
    int upgrades(UUID playerId);

    /** How many slots this player's backpack currently has. */
    int capacity(UUID playerId);

    /** Warms {@link #upgrades} on join. */
    CompletableFuture<Integer> loadUpgrades(UUID playerId);

    /**
     * Buys one more widening, paid for out of the feature-point pool.
     *
     * <p>The caller has already checked that the player can afford it - this
     * only enforces the hard ceiling, so a second click landing in the same tick
     * as the first cannot buy a third row.</p>
     *
     * @return the new upgrade count, unchanged when the ceiling was already
     *         reached
     */
    CompletableFuture<Integer> purchaseUpgrade(UUID playerId);

    void evict(UUID playerId);

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
