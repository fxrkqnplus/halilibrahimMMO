package com.halilibrahim.halilibrahimmmo.resourcepack;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Owns {@code kaynak-paketi.yml} and the per-join resource pack handshake.
 *
 * <p>Pack state is in-memory only, like {@code ModVerificationService} and
 * {@code PlayerEnergyService}: it describes a live connection, not a player's
 * progress, so it has no database table and no migration. Persisting "this
 * player already had the pack" would also be actively wrong - the client caches
 * by digest and renegotiates on every join anyway, and a stale row would wave
 * through a player still holding the previous pack's bytes.</p>
 */
public interface ResourcePackService extends PluginService {

    /** @return the current, immutable {@code kaynak-paketi.yml} snapshot. Safe from any thread. */
    ResourcePackSettings settings();

    /**
     * Offers the pack to a player who has just joined and arms the stall
     * watchdog. Does nothing when the pack is disabled or unusable; sends
     * without forcing, and never kicks, for a holder of the bypass permission.
     */
    void begin(Player player);

    /** Handles one status report from a client. Only terminal statuses decide anything. */
    void handleStatus(Player player, PlayerResourcePackStatusEvent.Status status);

    /** Drops all state for a player who has left, cancelling any pending decision. */
    void forget(UUID playerId);

    /** @return the admin-screen view of a player's state, empty if never started. */
    Optional<ResourcePackSnapshot> snapshot(UUID playerId);
}
