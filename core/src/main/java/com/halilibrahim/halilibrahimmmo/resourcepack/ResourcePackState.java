package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One connected player's resource pack state.
 *
 * <p>Mutated from the join listener, the player's region scheduler (the
 * watchdog) and Bukkit's status-event dispatch, so every field is either final
 * or volatile and the "who gets to decide this player's fate" race is settled
 * once by {@link #resolve()} - the same discipline as
 * {@code ModVerificationState}.</p>
 *
 * <p><strong>Where this deliberately differs from the mod handshake:</strong>
 * modcheck receives exactly one reply per player, so every incoming message may
 * resolve. A resource pack download reports <em>repeatedly</em> - {@code
 * ACCEPTED}, then {@code DOWNLOADED}, then {@code SUCCESSFULLY_LOADED} - so
 * only a terminal status is allowed to resolve. The intermediate ones call
 * {@link #touch()} instead, which is what keeps the watchdog from killing a
 * download that is simply slow.</p>
 *
 * <p>{@code DISCARDED} is the subtle one: the client also sends it when a pack
 * is removed or the connection ends, i.e. <em>after</em> a successful load. The
 * compare-and-set in {@link #resolve()} handles that for free - a player who
 * already reached {@link ResourcePackStatus#APPLIED} is resolved, so the late
 * {@code DISCARDED} is a no-op. Without it, a successful player would be kicked
 * on their way out and, on a reconnect loop, never get back in.</p>
 */
final class ResourcePackState {

    private final AtomicBoolean resolved = new AtomicBoolean();

    private volatile ResourcePackStatus status;
    private volatile PlayerResourcePackStatusEvent.Status lastReported;
    private volatile long lastProgressAtMillis;

    private ResourcePackState(ResourcePackStatus status) {
        this.status = status;
        this.lastProgressAtMillis = System.currentTimeMillis();
    }

    static ResourcePackState pending() {
        return new ResourcePackState(ResourcePackStatus.PENDING);
    }

    /**
     * An exempt player is created already resolved, so no watchdog and no
     * refusal can later kick them - but they are still sent the pack, so the
     * owner sees the art without disabling their own bypass.
     */
    static ResourcePackState exempt() {
        ResourcePackState state = new ResourcePackState(ResourcePackStatus.EXEMPT);
        state.resolved.set(true);
        return state;
    }

    /**
     * @return {@code true} for the single caller that wins the right to decide
     *         this handshake. Every later caller - a watchdog firing just after
     *         a success, a {@code DISCARDED} arriving on disconnect - gets
     *         {@code false} and must do nothing.
     */
    boolean resolve() {
        return resolved.compareAndSet(false, true);
    }

    boolean isResolved() {
        return resolved.get();
    }

    /** Records that the download moved, restarting the watchdog's patience. */
    void touch() {
        this.lastProgressAtMillis = System.currentTimeMillis();
    }

    /** @return true when nothing has happened for longer than the configured timeout. */
    boolean isStalled(long timeoutMillis) {
        return System.currentTimeMillis() - lastProgressAtMillis > timeoutMillis;
    }

    ResourcePackStatus status() {
        return status;
    }

    void markApplied() {
        this.status = ResourcePackStatus.APPLIED;
    }

    void lastReported(PlayerResourcePackStatusEvent.Status reported) {
        this.lastReported = reported;
    }

    ResourcePackSnapshot toSnapshot() {
        return new ResourcePackSnapshot(status, lastReported);
    }
}
