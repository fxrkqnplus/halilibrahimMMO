package com.halilibrahim.halilibrahimmmo.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A per-player "not again until" clock, deliberately in memory only.
 *
 * <p>Used by the two teleport commands ({@code /ev}, {@code /rtp}), which is
 * exactly why it is one small shared class rather than the same map written
 * twice. Nothing here is persisted: a cooldown that survives a restart would
 * punish players for something the server did, and the whole point of both
 * cooldowns is to pace a session rather than to be a long-term budget.</p>
 */
public final class PlayerCooldowns {

    private final long durationMillis;
    private final ConcurrentHashMap<UUID, Long> readyAtMillis = new ConcurrentHashMap<>();

    public PlayerCooldowns(long durationMillis) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("Cooldown duration cannot be negative.");
        }
        this.durationMillis = durationMillis;
    }

    /** How long this player still has to wait, or {@code 0} if they may act now. */
    public long remainingMillis(UUID playerId) {
        return Math.max(0L, readyAtMillis.getOrDefault(playerId, 0L) - System.currentTimeMillis());
    }

    /** Starts the clock again, from now. Called only after the action actually went through. */
    public void start(UUID playerId) {
        readyAtMillis.put(playerId, System.currentTimeMillis() + durationMillis);
    }

    public void evict(UUID playerId) {
        readyAtMillis.remove(playerId);
    }

    public void clear() {
        readyAtMillis.clear();
    }
}
