package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The other half of the transformation: hides the person so the wolf can stand
 * where they were.
 *
 * <p>Works exactly the way {@link EntityGlowTicker} does, and for exactly the
 * same reason - the flag is set on the client's <em>own copy</em> of the entity,
 * so nothing about the player's actual state on the server changes. Asil Kurt
 * does not grant invisibility: it grants a different body, and this is how a
 * client is told to stop drawing the old one.</p>
 *
 * <p>It also resolves the server's UUIDs into the entity ids
 * {@link WerewolfLayer} matches on, once a tick rather than once a frame.</p>
 *
 * <p><strong>Everything it touches is undone.</strong> A player who stops being
 * a wolf, walks out of range, or is left behind when the connection drops is
 * made visible again. A player who was <em>already</em> invisible when the
 * transformation started is remembered and left invisible afterwards - their
 * potion is not ours to cancel.</p>
 */
public final class WolfFormTicker {

    private final WolfFormState forms;
    /** Entity ids this class made invisible, so it only ever undoes its own work. */
    private final Set<Integer> hidden = new HashSet<>();

    public WolfFormTicker(WolfFormState forms) {
        this.forms = forms;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null) {
            hidden.clear();
            return;
        }
        if (forms.isEmpty()) {
            revealAll(level);
            forms.publishEntityIds(Set.of());
            return;
        }
        Set<UUID> wanted = forms.transformedPlayers();
        Set<Integer> nowTransformed = new HashSet<>();
        for (Player player : level.players()) {
            if (!wanted.contains(player.getUUID())) {
                continue;
            }
            nowTransformed.add(player.getId());
            if (hidden.add(player.getId())) {
                // Somebody already invisible when the wolf came out stays our
                // problem to leave alone: forget them again so the reveal pass
                // never turns somebody's potion off for them.
                if (player.isInvisible()) {
                    hidden.remove(player.getId());
                    continue;
                }
                player.setInvisible(true);
            }
        }
        hidden.removeIf(id -> {
            if (nowTransformed.contains(id)) {
                return false;
            }
            reveal(level, id);
            return true;
        });
        forms.publishEntityIds(nowTransformed);
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
