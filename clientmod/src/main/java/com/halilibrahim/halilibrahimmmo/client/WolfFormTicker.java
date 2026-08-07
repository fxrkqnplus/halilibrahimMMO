package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The other half of the transformation: hides the person so the wolf can stand
 * where they were, and works out what that wolf's body is doing.
 *
 * <p>The hiding is a flag set on the client's <em>own copy</em> of the entity,
 * so nothing about the player's actual state on the server changes. Asil Kurt
 * does not grant invisibility: it grants a different body, and this is how a
 * client is told to stop drawing the old one.</p>
 *
 * <p><strong>Re-applied every tick rather than once on sight.</strong> The
 * invisible bit lives in the same synched byte as "on fire", "sneaking",
 * "sprinting" and "swimming", and the server rewrites that whole byte whenever
 * any of them changes - so a wolf that started sprinting became a visible player
 * again for the rest of the minute. Setting it every tick costs one comparison
 * and is the only version of this that survives a player who moves.</p>
 *
 * <p>It also resolves the server's UUIDs into the entity ids
 * {@link WerewolfLayer} matches on, once a tick rather than once a frame, and
 * eases the motion weights {@link WerewolfModel} animates from.</p>
 *
 * <p><strong>Everything it touches is undone.</strong> A player who stops being
 * a wolf, walks out of range, or is left behind when the connection drops is
 * made visible again. A player who was <em>already</em> invisible when the
 * transformation started is remembered and left invisible afterwards - their
 * potion is not ours to cancel.</p>
 */
public final class WolfFormTicker {

    /** How fast a motion weight climbs into a new pose, per tick. A leap has to look thrown. */
    private static final float EASE_IN = 0.35F;
    /** How fast it settles back out of one. A landing is a body absorbing something. */
    private static final float EASE_OUT = 0.16F;
    /**
     * The vertical speed, in blocks per tick, that counts as "falling as fast as
     * this animation cares about".
     *
     * <p>Vanilla terminal velocity is about 3.9 blocks a tick, but a wolf
     * already looks fully committed to a fall long before that; measuring
     * against the whole range would leave every ordinary drop looking like a
     * gentle hop.</p>
     */
    private static final double FULL_RISE_PER_TICK = 0.55D;

    private final WolfFormState forms;
    /** Entity ids this class made invisible, so it only ever undoes its own work. */
    private final Set<Integer> hidden = new HashSet<>();
    /** Last tick's world Y per entity - the only reliable way to read another player's vertical speed. */
    private final Map<Integer, Double> lastY = new HashMap<>();
    /** Last tick's eased weights per entity, so the easing has something to ease from. */
    private final Map<Integer, WolfMotion> eased = new HashMap<>();

    public WolfFormTicker(WolfFormState forms) {
        this.forms = forms;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            hidden.clear();
            lastY.clear();
            eased.clear();
            return;
        }
        if (forms.isEmpty()) {
            revealAll(level);
            lastY.clear();
            eased.clear();
            forms.publishEntityIds(Set.of());
            return;
        }
        Set<UUID> wanted = forms.transformedPlayers();
        Set<Integer> nowTransformed = new HashSet<>();
        for (Player player : level.players()) {
            if (!wanted.contains(player.getUUID())) {
                continue;
            }
            int id = player.getId();
            nowTransformed.add(id);
            hide(player);
            forms.publishMotion(id, advance(player, id));
        }
        hidden.removeIf(id -> {
            if (nowTransformed.contains(id)) {
                return false;
            }
            reveal(level, id);
            return true;
        });
        lastY.keySet().retainAll(nowTransformed);
        eased.keySet().retainAll(nowTransformed);
        forms.publishEntityIds(nowTransformed);
    }

    /**
     * Hides one player, and keeps hiding them.
     *
     * <p>The "was already invisible" check runs only the first time we see them,
     * for the obvious reason: after we have hidden somebody, they are invisible,
     * and asking the question again would answer "leave them alone" about our own
     * work.</p>
     */
    private void hide(Player player) {
        if (hidden.add(player.getId())) {
            if (player.isInvisible()) {
                // Their potion, not ours. Forget them entirely so the reveal
                // pass never turns it off for them.
                hidden.remove(player.getId());
                return;
            }
            player.setInvisible(true);
            return;
        }
        if (!player.isInvisible()) {
            player.setInvisible(true);
        }
    }

    /**
     * Reads this wolf's raw state and eases last tick's weights toward it.
     *
     * <p>Vertical speed is measured from the position rather than read off
     * {@code getDeltaMovement}: for a player who is not the one holding the
     * mouse, that vector is whatever the last movement packet left behind, while
     * the position is interpolated every tick and is what the model is drawn
     * against anyway.</p>
     */
    private WolfMotion advance(Player player, int entityId) {
        Double previousY = lastY.put(entityId, player.getY());
        double verticalPerTick = previousY == null ? 0.0D : player.getY() - previousY;
        boolean airborne = !player.onGround();
        float rise = airborne
                ? (float) Math.max(-1.0D, Math.min(1.0D, verticalPerTick / FULL_RISE_PER_TICK))
                : 0.0F;
        WolfMotion target = new WolfMotion(
                airborne ? 1.0F : 0.0F,
                rise,
                airborne && player.isShiftKeyDown() ? 1.0F : 0.0F,
                player.isSprinting() ? 1.0F : 0.0F,
                !airborne && player.isCrouching() ? 1.0F : 0.0F
        );
        WolfMotion next = eased.getOrDefault(entityId, WolfMotion.RESTING)
                .easeToward(target, EASE_IN, EASE_OUT);
        eased.put(entityId, next);
        return next;
    }

    private void revealAll(ClientLevel level) {
        hidden.forEach(id -> reveal(level, id));
        hidden.clear();
    }

    private void reveal(ClientLevel level, int entityId) {
        if (level.getEntity(entityId) instanceof Player player) {
            player.setInvisible(false);
        }
    }
}
