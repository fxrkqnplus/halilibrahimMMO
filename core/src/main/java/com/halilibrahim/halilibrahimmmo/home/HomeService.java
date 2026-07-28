package com.halilibrahim.halilibrahimmmo.home;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * One named home per player, set where they stand and reachable from anywhere -
 * unlocked at class level {@code 15}, or immediately for anyone who has ever
 * prestiged.
 *
 * <p>The Nether and the End are excluded at both ends of the trip: a home
 * cannot be set in either, and cannot be travelled to from either. Letting
 * {@code /ev} short-circuit a nether portal - or a fall into the End - would
 * quietly delete the part of the game those dimensions are.</p>
 */
public interface HomeService extends PluginService {

    /** Longest home name a player may choose; long enough to be descriptive, short enough to fit a sidebar line. */
    int MAXIMUM_NAME_LENGTH = 32;

    /**
     * Synchronous, in-memory only - empty until {@link #load} has warmed this
     * player's entry on join. Backs the sidebar, which cannot await a future.
     */
    Optional<PlayerHome> cached(UUID playerId);

    /** Synchronous, in-memory only: whether this player's class progress has unlocked {@code /ev}. */
    boolean isUnlocked(UUID playerId);

    CompletableFuture<Optional<PlayerHome>> load(UUID playerId);

    /** Sets (or replaces) this player's home at their current location. */
    CompletableFuture<HomeActionResult> setHome(Player player, String name);

    /** Teleports the player to their home, if they have one and are somewhere they are allowed to leave from. */
    CompletableFuture<HomeActionResult> teleportHome(Player player);

    CompletableFuture<HomeActionResult> deleteHome(Player player);

    void evict(UUID playerId);
}
