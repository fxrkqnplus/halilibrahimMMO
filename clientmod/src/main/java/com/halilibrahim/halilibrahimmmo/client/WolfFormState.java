package com.halilibrahim.halilibrahimmmo.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who the server says is currently wearing Asil Kurt's body.
 *
 * <p>Separate from {@link AbilityHudState} on purpose: that class is about the
 * one ability <em>this</em> player is running, and this one is about everybody
 * else. A player sees their own wolf only in third person, and sees other
 * people's wolves whether or not they have ever taken the ability.</p>
 *
 * <p>Replaced wholesale on every message rather than added to and removed from,
 * because the server sends the whole set every time. That is what makes a
 * dropped packet self-correcting: there is no incremental state to fall out of
 * step, only a picture that is at most one message old.</p>
 *
 * <p><strong>Two views of the same set, and the reason is the render
 * path.</strong> The server can only name players by UUID, but a render state
 * carries the entity's network id and nothing else - resolving a UUID to an
 * entity inside {@code submit} would mean a scan of the level once per
 * transformed player per frame. {@link WolfFormTicker} does that resolution once
 * a tick instead and leaves the answer here as ids.</p>
 */
public final class WolfFormState {

    private final Set<UUID> transformed = ConcurrentHashMap.newKeySet();
    private final Set<Integer> transformedEntityIds = ConcurrentHashMap.newKeySet();
    /**
     * What each transformed body is doing, keyed by entity id.
     *
     * <p>Here rather than in {@link WerewolfModel} because one model instance
     * draws every wolf on screen - it has nowhere to keep a memory of any single
     * one of them. {@link WolfFormTicker} writes this once a tick and the render
     * layer reads it once per player per frame.</p>
     */
    private final Map<Integer, WolfMotion> motions = new ConcurrentHashMap<>();

    public void accept(Set<UUID> playerIds) {
        transformed.retainAll(playerIds);
        transformed.addAll(playerIds);
        if (transformed.isEmpty()) {
            transformedEntityIds.clear();
            motions.clear();
        }
    }

    public void clear() {
        transformed.clear();
        transformedEntityIds.clear();
        motions.clear();
    }

    public boolean isTransformed(UUID playerId) {
        return !transformed.isEmpty() && transformed.contains(playerId);
    }

    /** The whole set, for the ticker's once-a-tick resolution pass. */
    public Set<UUID> transformedPlayers() {
        return Set.copyOf(transformed);
    }

    /** Replaces the resolved ids. Called only by {@link WolfFormTicker}. */
    void publishEntityIds(Set<Integer> entityIds) {
        transformedEntityIds.retainAll(entityIds);
        transformedEntityIds.addAll(entityIds);
        motions.keySet().retainAll(entityIds);
    }

    /** Records one wolf's eased motion weights. Called only by {@link WolfFormTicker}. */
    void publishMotion(int entityId, WolfMotion motion) {
        motions.put(entityId, motion);
    }

    /**
     * What this wolf is doing, or {@link WolfMotion#RESTING} for one the ticker
     * has not seen yet - a body that has just come into view stands still for a
     * frame rather than being drawn in whatever pose the previous entity left.
     */
    public WolfMotion motionOf(int entityId) {
        return motions.getOrDefault(entityId, WolfMotion.RESTING);
    }

    /** What the render layer asks, once per player per frame. */
    public boolean isTransformedEntity(int entityId) {
        return !transformedEntityIds.isEmpty() && transformedEntityIds.contains(entityId);
    }

    /** {@code true} when nobody is transformed, so a per-tick or per-frame pass can bail in one check. */
    public boolean isEmpty() {
        return transformed.isEmpty();
    }
}
