package com.halilibrahim.halilibrahimmmo.client;

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

    public void accept(Set<UUID> playerIds) {
        transformed.retainAll(playerIds);
        transformed.addAll(playerIds);
        if (transformed.isEmpty()) {
            transformedEntityIds.clear();
        }
    }

    public void clear() {
        transformed.clear();
        transformedEntityIds.clear();
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
