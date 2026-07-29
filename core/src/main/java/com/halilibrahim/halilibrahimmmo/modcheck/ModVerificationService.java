package com.halilibrahim.halilibrahimmmo.modcheck;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Owns {@code modlar.yml} and the per-join Fabric mod handshake.
 *
 * <p>Verification state is intentionally in-memory only, like
 * {@code PlayerEnergyService}: it describes a live connection, not a player's
 * progress, so it has no database table, no migration, and is rebuilt from
 * scratch on every join.</p>
 */
public interface ModVerificationService extends PluginService {

    /** @return the current, immutable {@code modlar.yml} snapshot. Safe from any thread. */
    ModVerificationSettings settings();

    /**
     * Starts the handshake for a player who has just joined: optionally checks
     * the client brand, sends the challenge, and arms the timeout. Does nothing
     * when verification is disabled or the player holds the bypass permission.
     */
    void begin(Player player);

    /** Handles one raw {@code hmmo:modcheck} payload from a client. */
    void handleResponse(Player player, byte[] payload);

    /** Drops all state for a player who has left, cancelling any pending decision. */
    void forget(UUID playerId);

    /** @return the admin-screen view of a player's state, empty if never started. */
    Optional<ModVerificationSnapshot> snapshot(UUID playerId);
}
