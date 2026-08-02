package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Makes the hostile creatures around the player glow while an ability that asks
 * for it is running - Kan Yemini, so far.
 *
 * <p><strong>This has to be done on the client and cannot be done on the
 * server.</strong> Bukkit's glow flag is a property of the entity, not of a
 * viewer, so a server-side version would light the mobs up for everybody in
 * render distance and leave them lit for players who have never taken the oath.
 * Here the flag is set on the client's <em>own copy</em> of the entity, which is
 * exactly as private as the screen tint it belongs with.</p>
 *
 * <p>The colour comes from a client-side scoreboard team, because that is the
 * only thing vanilla's outline renderer reads a colour from. The team is created
 * locally, is never sent anywhere, and carries a name no server would use.</p>
 *
 * <p>Everything it touches is undone: every entity it marked is unmarked when
 * the ability ends, when the player leaves the world, or when the entity walks
 * out of range. A creature that was <em>already</em> glowing when the ability
 * started is left alone entirely - it is not ours to turn off.</p>
 */
public final class HostileGlowTicker {

    /** How far the oath reaches, in blocks. Matches nothing on the server: this is presentation only. */
    private static final double RADIUS = 32.0D;
    /**
     * Inside this fraction of {@link #RADIUS} a creature burns in the bright
     * team rather than the dark one.
     *
     * <p><strong>Two bands, not a gradient, and that is a genuine limitation of
     * the game rather than a choice.</strong> Vanilla's outline renderer reads
     * its colour from a scoreboard team and from nowhere else, and a team's
     * colour is one of the sixteen chat colours - there is no per-entity alpha
     * to fade. Dark red for the ones still out there and bright red for the ones
     * on top of you is the whole range the renderer can express. The rest of
     * "closer is worse" is carried by the screen tint, which <em>can</em> be
     * scaled smoothly.</p>
     */
    private static final double NEAR_BAND = 0.45D;
    private static final String TEAM_NAME = "hmmo_kan_yemini";
    private static final String NEAR_TEAM_NAME = "hmmo_kan_yemini_yakin";

    private final AbilityHudState state;
    private final Set<Integer> marked = new HashSet<>();

    public HostileGlowTicker(AbilityHudState state) {
        this.state = state;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            forgetAll(null);
            return;
        }
        if (!state.highlightsHostiles()) {
            forgetAll(level);
            return;
        }
        PlayerTeam farTeam = ensureTeam(level.getScoreboard(), TEAM_NAME, TeamColor.DARK_RED);
        PlayerTeam nearTeam = ensureTeam(level.getScoreboard(), NEAR_TEAM_NAME, TeamColor.RED);
        boolean banded = state.glowScalesWithDistance();
        Set<Integer> stillNear = new HashSet<>();
        double radiusSquared = RADIUS * RADIUS;
        double nearSquared = radiusSquared * NEAR_BAND * NEAR_BAND;
        for (Entity entity : level.entitiesForRendering()) {
            double distanceSquared = entity.distanceToSqr(minecraft.player);
            if (!(entity instanceof Enemy) || entity.isRemoved() || distanceSquared > radiusSquared) {
                continue;
            }
            stillNear.add(entity.getId());
            PlayerTeam wanted = !banded || distanceSquared <= nearSquared ? nearTeam : farTeam;
            if (marked.add(entity.getId())) {
                // Only ever turned on for a creature that was not already
                // glowing on its own - otherwise ending the ability would put
                // out somebody else's spectral arrow.
                if (entity.hasGlowingTag()) {
                    marked.remove(entity.getId());
                    stillNear.remove(entity.getId());
                    continue;
                }
                entity.setGlowingTag(true);
            }
            // Re-applied every tick rather than only on first sight: a creature
            // that walks from the far band into the near one has to change
            // colour, and moving it between teams is the only way to say so.
            level.getScoreboard().addPlayerToTeam(entity.getScoreboardName(), wanted);
        }
        marked.removeIf(id -> {
            if (stillNear.contains(id)) {
                return false;
            }
            unmark(level, id);
            return true;
        });
    }

    private void forgetAll(ClientLevel level) {
        if (level != null) {
            marked.forEach(id -> unmark(level, id));
        }
        marked.clear();
    }

    private void unmark(ClientLevel level, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        entity.setGlowingTag(false);
        level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());
    }

    /**
     * A local team whose only purpose is to carry a colour. Created on the
     * client's own scoreboard, so it is never sent anywhere and never collides
     * with the server's own teams.
     */
    private PlayerTeam ensureTeam(Scoreboard scoreboard, String name, TeamColor color) {
        PlayerTeam existing = scoreboard.getPlayerTeam(name);
        PlayerTeam team = existing != null ? existing : scoreboard.addPlayerTeam(name);
        team.setColor(Optional.of(color));
        return team;
    }
}
