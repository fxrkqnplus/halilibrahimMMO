package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Makes the creatures - and, for one ability, the people - around the player
 * glow while an ability that asks for it is running.
 *
 * <p><strong>This has to be done on the client and cannot be done on the
 * server.</strong> Bukkit's glow flag is a property of the entity, not of a
 * viewer, so a server-side version would light things up for everybody in
 * render distance and leave them lit for players who have never taken the oath.
 * Here the flag is set on the client's <em>own copy</em> of the entity, which is
 * exactly as private as the screen tint it belongs with.</p>
 *
 * <h2>Why this never worked before Faz 25, and what fixed it</h2>
 * <p>Every version of this class up to Faz 24 called
 * {@code Entity#setGlowingTag(true)} and nothing lit up. That method is a no-op
 * on the client, and the reason is one line of vanilla:</p>
 * <pre>
 * public final void setGlowingTag(boolean glowing) {
 *     this.hasGlowingTag = glowing;
 *     this.setSharedFlag(6, this.isCurrentlyGlowing());   // &lt;-- reads, not writes
 * }
 * public boolean isCurrentlyGlowing() {
 *     return this.level().isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
 * }
 * </pre>
 * <p>On a client level {@code isCurrentlyGlowing()} answers with shared flag 6
 * itself, so the setter writes the flag's <em>current</em> value back over it.
 * The field it does set, {@code hasGlowingTag}, is the one the client never
 * reads. Both halves were read out of the compiled 26.2 jar rather than assumed,
 * which is also how the guard below was found to be broken in the same way:
 * {@code hasGlowingTag()} is always {@code false} on a client, so "leave an
 * entity that was already glowing alone" was never actually leaving anything
 * alone.</p>
 *
 * <p>The fix is to write shared flag 6 directly. {@code Entity#setSharedFlag} is
 * {@code protected} and {@code DATA_SHARED_FLAGS_ID} is not public either, but
 * {@code Entity#getEntityData()} is, {@link EntityDataAccessor} is a public
 * record, and the accessor's id is simply its position in the entity data table
 * - and shared flags are the very first thing {@code Entity} defines, so that
 * position is zero. This buys the fix with no mixin, no access widener and no
 * split package; {@link #SHARED_FLAGS_VERIFIED} is the honesty check that the
 * assumption still holds on whatever version is actually running.</p>
 *
 * <p><strong>Which entities count is the server's decision, carried in three
 * independent flags.</strong> Kan Yemini lights up monsters and ordinary
 * creatures and never players; Asil Kurt lights up ordinary creatures and
 * players and never monsters - which is not an oversight but the ability's own
 * logic, since it has already made every monster blind to its owner. Doing it
 * with three bits rather than one boolean is what lets the two abilities look
 * genuinely different without the client knowing either of their names.</p>
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
public final class EntityGlowTicker {

    private static final Logger LOGGER = LoggerFactory.getLogger("hmmo-client");

    /** How far the effect reaches, in blocks. Matches nothing on the server: this is presentation only. */
    private static final double RADIUS = 32.0D;
    /**
     * Inside this fraction of {@link #RADIUS} an entity burns in the bright team
     * rather than the dark one.
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
    private static final String TEAM_NAME = "hmmo_av_uzak";
    private static final String NEAR_TEAM_NAME = "hmmo_av_yakin";

    /**
     * Shared flag 6 is "glowing" - the same bit
     * {@code Entity.FLAG_GLOWING} names on the server side.
     */
    private static final int GLOWING_FLAG = 6;
    /**
     * The synched-data slot the shared flags byte lives in.
     *
     * <p>Zero because {@code Entity} defines it first and ids are handed out in
     * declaration order from the root of the hierarchy down. Asserted rather
     * than trusted - see {@link #canGlow(Entity)}.</p>
     */
    private static final EntityDataAccessor<Byte> SHARED_FLAGS =
            new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);

    /**
     * Whether writing shared flag 6 actually produced a glowing entity.
     *
     * <p>Three states rather than a boolean: unknown until the first entity is
     * tried, then either working or not. If a future game version moves the
     * shared flags out of slot zero this turns the feature off and says so once,
     * instead of silently painting some other flag onto every creature in
     * range.</p>
     */
    private enum GlowSupport { UNKNOWN, WORKING, UNSUPPORTED }

    private final AbilityHudState state;
    private final Set<Integer> marked = new HashSet<>();
    private GlowSupport support = GlowSupport.UNKNOWN;

    public EntityGlowTicker(AbilityHudState state) {
        this.state = state;
    }

    public void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            forgetAll(null);
            return;
        }
        if (!state.glowsAnything() || support == GlowSupport.UNSUPPORTED) {
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
            if (entity.isRemoved() || distanceSquared > radiusSquared || !wanted(entity, minecraft.player)) {
                continue;
            }
            // Only ever turned on for an entity that was not already glowing on
            // its own - otherwise ending the ability would put out somebody
            // else's spectral arrow. Read through isCurrentlyGlowing(), which is
            // the one that answers truthfully on a client.
            if (!marked.contains(entity.getId()) && entity.isCurrentlyGlowing()) {
                continue;
            }
            if (!canGlow(entity)) {
                return;
            }
            marked.add(entity.getId());
            stillNear.add(entity.getId());
            // Re-applied every tick rather than only on first sight, and that is
            // load-bearing twice over: an entity crossing from the far band into
            // the near one has to change team, and any entity-data update the
            // server sends (catching fire, starting to sneak) rewrites the whole
            // flags byte and would otherwise put the glow out until the ability
            // ended.
            PlayerTeam wanted = !banded || distanceSquared <= nearSquared ? nearTeam : farTeam;
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

    /**
     * Turns the glow bit on and confirms the game agreed.
     *
     * @return {@code false} once, and from then on for good, if writing the flag
     *         did not make the entity glow - which can only mean the shared flags
     *         are no longer in slot zero
     */
    private boolean canGlow(Entity entity) {
        setGlowing(entity, true);
        if (support == GlowSupport.WORKING) {
            return true;
        }
        if (entity.isCurrentlyGlowing()) {
            support = GlowSupport.WORKING;
            return true;
        }
        support = GlowSupport.UNSUPPORTED;
        LOGGER.warn("Parlama efekti bu Minecraft surumunde calismiyor: varlik veri yuvasi 0 artik "
                + "paylasimli bayraklari tasimiyor. Kabiliyetlerin geri kalani etkilenmez.");
        return false;
    }

    /** Writes shared flag 6 on the client's own copy of the entity. See the class note for why not {@code setGlowingTag}. */
    private static void setGlowing(Entity entity, boolean glowing) {
        try {
            byte flags = entity.getEntityData().get(SHARED_FLAGS);
            byte updated = glowing
                    ? (byte) (flags | (1 << GLOWING_FLAG))
                    : (byte) (flags & ~(1 << GLOWING_FLAG));
            if (updated != flags) {
                entity.getEntityData().set(SHARED_FLAGS, updated);
            }
        } catch (RuntimeException exception) {
            // A slot that is not a byte, or an entity whose data table is not
            // built yet. Neither is worth a crash for a red outline.
            LOGGER.debug("Parlama bayragi yazilamadi.", exception);
        }
    }

    /**
     * Whether this entity is in one of the three sets the running ability asked
     * for.
     *
     * <p>The viewer is excluded explicitly. A player under Asil Kurt is a player
     * within range of themselves, and lighting up your own outline would put a
     * red silhouette over the middle of your screen for a minute.</p>
     */
    private boolean wanted(Entity entity, Player viewer) {
        if (entity == viewer) {
            return false;
        }
        if (entity instanceof Player) {
            return state.glowsPlayers();
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        return entity instanceof Enemy ? state.glowsMonsters() : state.glowsCreatures();
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
        setGlowing(entity, false);
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
