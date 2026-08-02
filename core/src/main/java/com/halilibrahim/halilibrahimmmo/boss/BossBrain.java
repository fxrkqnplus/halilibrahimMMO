package com.halilibrahim.halilibrahimmmo.boss;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What a world boss decides to do, half a second at a time.
 *
 * <p>Everything here runs on the boss's own entity scheduler, which is the only
 * thread allowed to read its health and position or to move it - the same rule
 * every other entity-touching path in this plugin follows. Nothing is stored in
 * this class; the per-boss memory lives in {@link BossState}, so a single
 * instance drives every boss on the server.</p>
 *
 * <p><strong>One phase per tick, not every rule in sequence.</strong> The
 * previous behaviour loop healed, blinked and fled as three independent
 * {@code if} blocks, so a badly hurt boss teleported away and then immediately
 * repositioned itself back toward the player who had hurt it - the escape it was
 * meant to be undid itself in the same half second. {@link BossPhase} makes the
 * choice explicit and exclusive.</p>
 *
 * <p><strong>Every block this class changes goes through the event a vanilla mob
 * would raise</strong> ({@link EntityBreakDoorEvent} for a door,
 * {@link EntityChangeBlockEvent} for a crop) and is abandoned if anything
 * cancels it, and nothing happens at all where the {@code mobGriefing} gamerule
 * is off. A boss is meant to be dangerous to a village, not to a server owner's
 * region protection.</p>
 */
public final class BossBrain {

    /** How far a boss looks for somebody to fight. */
    private static final double HUNT_RADIUS = 40.0D;
    /** How far it looks for a village worth going to. */
    private static final double VILLAGE_RADIUS = 48.0D;
    /** How far it looks for something harmless to take its boredom out on. */
    private static final double PREY_RADIUS = 20.0D;
    /** How far away is far enough to stop running and start mending. */
    private static final double ESCAPE_DISTANCE = 28.0D;
    /** How far a fleeing boss throws itself in one jump. */
    private static final int FLEE_DISTANCE = 20;
    /** How far a blink repositions during a fight. */
    private static final int BLINK_DISTANCE = 6;
    /** How far a boss wanders from where it is when it has nothing else to do. */
    private static final int ROAM_DISTANCE = 28;

    private static final long BLINK_INTERVAL_MILLIS = 12_000L;
    private static final long MELEE_INTERVAL_MILLIS = 1_200L;
    private static final long AIMED_SHOT_INTERVAL_MILLIS = 2_500L;
    private static final long ROAM_INTERVAL_MILLIS = 8_000L;
    private static final long VANDALISM_INTERVAL_MILLIS = 1_500L;

    private static final double WALK_SPEED = 1.0D;
    private static final double CHARGE_SPEED = 1.35D;
    private static final double FLEE_SPEED = 1.6D;

    /** How close a boss has to be for its own swing to land. */
    private static final double STRIKE_RANGE = 2.8D;
    /** Closer than this and a bow-armed boss backs off instead of shooting. */
    private static final double KITE_MINIMUM_RANGE = 8.0D;
    /** Beyond this an aimed shot is not worth taking. */
    private static final double KITE_MAXIMUM_RANGE = 32.0D;
    /** How fast an aimed shot flies, and how hard it hits, relative to a vanilla arrow. */
    private static final float AIMED_SHOT_SPEED = 3.2F;
    private static final double AIMED_SHOT_DAMAGE = 9.0D;

    /** Blocks of headroom below which a spot counts as a cave rather than a place to stand. */
    private static final int MINIMUM_HEADROOM = 4;
    /** How far below the surface a boss has to be before it counts as underground. */
    private static final int UNDERGROUND_DEPTH = 6;
    /** A drop this deep within one block counts as a ledge. */
    private static final int LEDGE_DROP = 5;

    /** Doors and crops broken per pass, so a raid is a siege rather than one frame of lag. */
    private static final int VANDALISM_PER_PASS = 3;
    private static final double VANDALISM_RADIUS = 3.0D;

    /** How high a boss hops when something a player could step over is in its way. */
    private static final double JUMP_VELOCITY = 0.46D;
    /** How fast it hauls itself up a ladder or a vine. */
    private static final double CLIMB_VELOCITY = 0.22D;

    private BossBrain() {
        throw new UnsupportedOperationException("BossBrain is a static utility class");
    }

    /**
     * One behaviour pass. Picks a phase, acts on it, and then applies the
     * movement help that every phase needs - a boss that cannot get over a fence
     * is not smarter for having decided where to go.
     */
    static void tick(Mob mob, BossState state, BossRaritySettings tier) {
        BossIntelligence mind = tier.intelligence();
        long now = System.currentTimeMillis();
        double ratio = healthRatio(mob);

        state.enterPhase(choosePhase(mob, state, tier, mind, ratio));
        switch (state.phase()) {
            case FLEEING -> flee(mob, state, now);
            case RECOVERING -> recover(mob, state, tier, now);
            case HUNTING -> hunt(mob, state, tier, mind, now);
            case RAIDING -> raid(mob, state, mind, now);
            case ROAMING -> roam(mob, state, mind, now);
        }
        if (mind.climbs()) {
            assistMovement(mob);
        }
        if (mind.opensGates()) {
            openObstacles(mob);
        }
    }

    // ------------------------------------------------------------------
    // Phase selection
    // ------------------------------------------------------------------

    /**
     * The single decision the rest of the tick obeys.
     *
     * <p>Order matters and is the whole design: running beats everything,
     * refusing to come back until mended beats being provoked, a player beats a
     * village, and a village beats wandering. A boss that answered a player
     * while at a tenth of its health would never actually escape, and one that
     * came back the instant somebody stepped near would never actually mend -
     * which is exactly what "flees at a quarter health" used to mean in
     * practice.</p>
     */
    private static BossPhase choosePhase(
            Mob mob, BossState state, BossRaritySettings tier, BossIntelligence mind, double ratio
    ) {
        boolean canRun = mind.recovers() && tier.escapes() > state.escapesUsed();
        if (state.phase() == BossPhase.FLEEING) {
            Player chaser = nearestPlayer(mob, ESCAPE_DISTANCE);
            return chaser == null ? BossPhase.RECOVERING : BossPhase.FLEEING;
        }
        if (state.phase() == BossPhase.RECOVERING) {
            if (ratio >= tier.recoveryRatio()) {
                return BossPhase.HUNTING;
            }
            // Cornered while mending: there is no point pretending to be calm
            // about somebody who has walked up and started swinging again.
            return nearestPlayer(mob, STRIKE_RANGE * 2) == null ? BossPhase.RECOVERING : BossPhase.HUNTING;
        }
        if (canRun && ratio <= tier.fleeRatio()) {
            return BossPhase.FLEEING;
        }
        if (nearestPlayer(mob, HUNT_RADIUS) != null) {
            return BossPhase.HUNTING;
        }
        if (mind.raidsVillages() && findRaidTarget(mob) != null) {
            return BossPhase.RAIDING;
        }
        return BossPhase.ROAMING;
    }

    // ------------------------------------------------------------------
    // Phases
    // ------------------------------------------------------------------

    /** Get away, and make it obvious that is what is happening. */
    private static void flee(Mob mob, BossState state, long now) {
        if (state.escapesUsed() == 0 || now - state.lastTeleportMillis() >= BLINK_INTERVAL_MILLIS) {
            if (state.escapesUsed() == 0) {
                state.markEscaped();
            }
            blink(mob, FLEE_DISTANCE, true);
            state.markTeleported(now);
        }
        mob.setTarget(null);
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2, false, false));
        Player chaser = nearestPlayer(mob, HUNT_RADIUS);
        if (chaser != null) {
            Vector away = mob.getLocation().toVector().subtract(chaser.getLocation().toVector());
            if (away.lengthSquared() > 0) {
                mob.getPathfinder().moveTo(mob.getLocation().add(away.normalize().multiply(12)), FLEE_SPEED);
            }
        }
    }

    /**
     * Mending, deliberately slowly and deliberately not all the way.
     *
     * <p><strong>This is where the reported bug was.</strong> The old loop
     * applied {@code REGENERATION} and spawned heart particles - but every
     * shipped archetype except the witch and the vindicator is undead, and
     * undead mobs are immune to Regeneration in Minecraft. The hearts appeared
     * and the health bar never moved, which is exactly what was reported.
     * Health is now written directly, which no immunity can intercept.</p>
     *
     * <p>The rate is a fraction of the boss's own maximum per second, so a
     * Mistik's recovery takes about as long as an ordinary boss's rather than
     * being instant on one and hopeless on the other. It stops at
     * {@code toparlanma-orani} rather than at full: a boss that comes back
     * untouched has erased the fight instead of surviving it.</p>
     */
    private static void recover(Mob mob, BossState state, BossRaritySettings tier, long now) {
        mob.setTarget(null);
        AttributeInstance maximum = mob.getAttribute(Attribute.MAX_HEALTH);
        if (maximum == null) {
            return;
        }
        double cap = maximum.getValue() * tier.recoveryRatio();
        if (mob.getHealth() < cap) {
            // The tick is a fraction of a second, so the configured per-second
            // rate is scaled to it rather than applied whole.
            double perTick = maximum.getValue() * tier.healthRegenPerSecond() * BossService.BEHAVIOUR_PERIOD_SECONDS;
            mob.setHealth(Math.min(cap, mob.getHealth() + perTick));
            mob.getWorld().spawnParticle(Particle.HEART, mob.getLocation().add(0, 1.2D, 0), 3, 0.4D, 0.4D, 0.4D, 0.0D);
        }
        // It keeps moving while it mends. Standing still would make "it ran
        // away" read as "it got stuck".
        wander(mob, state, now, true);
    }

    /** Somebody is here. Close the distance, or open it, depending on what is in hand. */
    private static void hunt(Mob mob, BossState state, BossRaritySettings tier, BossIntelligence mind, long now) {
        Player quarry = nearestPlayer(mob, HUNT_RADIUS);
        if (quarry == null) {
            return;
        }
        mob.setTarget(quarry);
        double distance = mob.getLocation().distance(quarry.getLocation());
        if (mind.kites() && isRangedArmed(mob)) {
            kite(mob, state, quarry, distance, now);
            return;
        }
        mob.getPathfinder().moveTo(quarry.getLocation(), CHARGE_SPEED);
        if (tier.teleports() && distance > BLINK_DISTANCE * 2 && now - state.lastTeleportMillis() >= BLINK_INTERVAL_MILLIS) {
            blink(mob, BLINK_DISTANCE, false);
            state.markTeleported(now);
        }
        if (mind.breaksDoors()) {
            breakObstructingDoor(mob, quarry.getLocation());
        }
    }

    /**
     * Bow in hand and the sense to use it.
     *
     * <p>Vanilla's own bow goal is left running underneath this and does the
     * ordinary shooting; what is added here is the two things it does not do -
     * backing out of melee range, and a heavy aimed shot on its own clock.
     * <strong>A crossbow is the reason this exists:</strong> an
     * {@code AbstractSkeleton} only swaps to its bow goal when it is holding a
     * {@code BOW} specifically, so the higher rarities - which used to be given
     * crossbows - silently fell back to the melee goal and walked up to punch
     * people while carrying a ranged weapon. {@code bosslar.yml} now gives every
     * tier a bow, and this covers any weapon the goal would refuse anyway.</p>
     */
    private static void kite(Mob mob, BossState state, Player quarry, double distance, long now) {
        if (distance < KITE_MINIMUM_RANGE) {
            Vector away = mob.getLocation().toVector().subtract(quarry.getLocation().toVector());
            if (away.lengthSquared() > 0) {
                mob.getPathfinder().moveTo(mob.getLocation().add(away.normalize().multiply(10)), FLEE_SPEED);
            }
        } else if (distance > KITE_MAXIMUM_RANGE) {
            mob.getPathfinder().moveTo(quarry.getLocation(), CHARGE_SPEED);
        }
        if (distance > KITE_MAXIMUM_RANGE || now - state.lastShotMillis() < AIMED_SHOT_INTERVAL_MILLIS) {
            return;
        }
        if (!mob.hasLineOfSight(quarry)) {
            return;
        }
        state.markShot(now);
        Location eye = mob.getEyeLocation();
        Vector aim = quarry.getEyeLocation().toVector().subtract(eye.toVector());
        if (aim.lengthSquared() == 0) {
            return;
        }
        Arrow shot = mob.launchProjectile(Arrow.class, aim.normalize().multiply(AIMED_SHOT_SPEED));
        shot.setDamage(AIMED_SHOT_DAMAGE);
        shot.setCritical(true);
        shot.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.2F, 0.7F);
    }

    /**
     * A village, and nobody to answer for it.
     *
     * <p>The order is the one a raid actually has: the golem is what would stop
     * this, so it goes first; then whatever is between the boss and the people;
     * then the people, young included; and the fields on the way through. Each
     * pass is capped and clocked, so a village is dismantled over minutes rather
     * than in a single frame.</p>
     */
    private static void raid(Mob mob, BossState state, BossIntelligence mind, long now) {
        LivingEntity quarry = findRaidTarget(mob);
        if (quarry == null) {
            return;
        }
        mob.setTarget(quarry);
        mob.getPathfinder().moveTo(quarry.getLocation(), CHARGE_SPEED);
        if (mind.breaksDoors()) {
            breakObstructingDoor(mob, quarry.getLocation());
        }
        strike(mob, state, quarry, now);
        if (now - state.lastVandalismMillis() >= VANDALISM_INTERVAL_MILLIS) {
            state.markVandalised(now);
            trampleCrops(mob);
        }
    }

    /**
     * Nothing to fight, so: walk somewhere, and take it out on whatever is
     * grazing on the way.
     *
     * <p>The livestock half is what makes an idle boss visible from a distance -
     * a herd scattering is how a player finds out something is out there before
     * the coordinates in chat mean anything to them.</p>
     */
    private static void roam(Mob mob, BossState state, BossIntelligence mind, long now) {
        if (mind.hunts()) {
            LivingEntity prey = findPrey(mob);
            if (prey != null) {
                mob.setTarget(prey);
                mob.getPathfinder().moveTo(prey.getLocation(), CHARGE_SPEED);
                strike(mob, state, prey, now);
                return;
            }
        }
        wander(mob, state, now, mind.avoidsHazards());
    }

    // ------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------

    /**
     * Picks somewhere to be and walks there, choosing a fresh destination when
     * the old one is reached or has gone stale.
     *
     * <p>{@code avoidHazards} is what keeps a clever boss out of caves and off
     * ledges. It deliberately only applies to wandering: a boss <em>fighting</em>
     * somebody who ran into a cave still follows them in, because refusing to
     * would turn every cave mouth into a safe room.</p>
     */
    private static void wander(Mob mob, BossState state, long now, boolean avoidHazards) {
        Location target = state.roamTarget();
        boolean stale = target == null
                || !target.getWorld().equals(mob.getWorld())
                || target.distanceSquared(mob.getLocation()) < 9.0D
                || now - state.lastRoamMillis() >= ROAM_INTERVAL_MILLIS;
        if (stale) {
            target = pickRoamTarget(mob, avoidHazards);
            state.setRoamTarget(target);
            state.markRoamed(now);
        }
        if (target != null) {
            mob.getPathfinder().moveTo(target, WALK_SPEED);
        }
    }

    /** A reachable, open point to walk to, or {@code null} if none of the tries were any good. */
    private static Location pickRoamTarget(Mob mob, boolean avoidHazards) {
        World world = mob.getWorld();
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = mob.getLocation().getBlockX() + ThreadLocalRandom.current().nextInt(-ROAM_DISTANCE, ROAM_DISTANCE + 1);
            int z = mob.getLocation().getBlockZ() + ThreadLocalRandom.current().nextInt(-ROAM_DISTANCE, ROAM_DISTANCE + 1);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (!avoidHazards || !isHazardous(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Whether a spot is a cave, a crawlspace or a ledge - the three places a
     * boss with any sense does not wander into.
     *
     * <p>All three are answered from block reads alone rather than from any
     * pathfinding query, because the question is asked about a point the boss is
     * not standing at yet. "Underground" is the surface being well above it,
     * "enclosed" is there being no headroom, and "a ledge" is the ground falling
     * away within one block of it.</p>
     */
    private static boolean isHazardous(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (world.getHighestBlockYAt(x, z) > y + UNDERGROUND_DEPTH) {
            return true;
        }
        for (int above = 1; above <= MINIMUM_HEADROOM; above++) {
            if (!world.getBlockAt(x, y + above, z).getType().isAir()) {
                return true;
            }
        }
        for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int neighbourX = x + step[0];
            int neighbourZ = z + step[1];
            if (world.getHighestBlockYAt(neighbourX, neighbourZ) < y - LEDGE_DROP) {
                return true;
            }
        }
        return false;
    }

    /**
     * The half of "moves like a player" that vanilla pathfinding does not cover:
     * hopping something a player would hop, and hauling itself up a ladder or a
     * vine it is already inside.
     */
    private static void assistMovement(Mob mob) {
        Location feet = mob.getLocation();
        if (Tag.CLIMBABLE.isTagged(feet.getBlock().getType())) {
            LivingEntity target = mob.getTarget();
            if (target == null || target.getLocation().getY() > feet.getY()) {
                mob.setVelocity(mob.getVelocity().setY(CLIMB_VELOCITY));
            }
            return;
        }
        if (!mob.isOnGround()) {
            return;
        }
        Vector heading = mob.getVelocity().setY(0);
        if (heading.lengthSquared() < 0.0025D) {
            return;
        }
        Location ahead = feet.clone().add(heading.normalize().multiply(0.8D));
        Block wall = ahead.getBlock();
        Block over = wall.getRelative(0, 1, 0);
        // Exactly the shape a player steps up: something solid at foot height
        // with open air over it. Two blocks high is a wall, and a wall is
        // something to walk around or break, not to jump.
        if (wall.getType().isSolid() && over.getType().isAir()) {
            mob.setVelocity(mob.getVelocity().setY(JUMP_VELOCITY));
        }
    }

    /**
     * Opens the trapdoor or fence gate the boss is standing against.
     *
     * <p>Opening is separated from breaking on purpose. A trapdoor or a gate is
     * something a player opens and walks through, so a boss doing the same reads
     * as cleverness; a front door reduced to splinters reads as a siege. The two
     * belong to different intelligence levels for exactly that reason.</p>
     */
    private static void openObstacles(Mob mob) {
        Location feet = mob.getLocation();
        for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}}) {
            for (int height = 0; height <= 1; height++) {
                Block block = feet.clone().add(step[0], height, step[1]).getBlock();
                if (!Tag.TRAPDOORS.isTagged(block.getType()) && !Tag.FENCE_GATES.isTagged(block.getType())) {
                    continue;
                }
                if (!(block.getBlockData() instanceof Openable openable) || openable.isOpen()) {
                    continue;
                }
                openable.setOpen(true);
                block.setBlockData(openable);
                block.getWorld().playSound(block.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1.0F, 0.8F);
                return;
            }
        }
    }

    /**
     * Takes the door between the boss and where it is trying to get to out of
     * the way permanently.
     *
     * <p>Raised as {@link EntityBreakDoorEvent}, which is precisely the event a
     * vanilla zombie raises when it does this - so every protection plugin that
     * already stops zombies eating a base's front door stops this too, without
     * knowing anything about this plugin.</p>
     */
    private static void breakObstructingDoor(Mob mob, Location towards) {
        if (!mayGrief(mob.getWorld())) {
            return;
        }
        Vector heading = towards.toVector().subtract(mob.getLocation().toVector()).setY(0);
        if (heading.lengthSquared() == 0) {
            return;
        }
        Location ahead = mob.getLocation().add(heading.normalize().multiply(1.2D));
        for (int height = 0; height <= 1; height++) {
            Block block = ahead.clone().add(0, height, 0).getBlock();
            if (!Tag.DOORS.isTagged(block.getType())) {
                continue;
            }
            EntityBreakDoorEvent event =
                    new EntityBreakDoorEvent(mob, block, Material.AIR.createBlockData());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 0.8F);
            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D),
                    24, 0.3D, 0.5D, 0.3D, block.getBlockData());
            block.setType(Material.AIR, false);
            return;
        }
    }

    /** Ruins what is growing where the boss is walking, a handful of stalks at a time. */
    private static void trampleCrops(Mob mob) {
        if (!mayGrief(mob.getWorld())) {
            return;
        }
        int ruined = 0;
        int radius = (int) VANDALISM_RADIUS;
        Location centre = mob.getLocation();
        for (int dx = -radius; dx <= radius && ruined < VANDALISM_PER_PASS; dx++) {
            for (int dz = -radius; dz <= radius && ruined < VANDALISM_PER_PASS; dz++) {
                for (int dy = -1; dy <= 1 && ruined < VANDALISM_PER_PASS; dy++) {
                    Block block = centre.clone().add(dx, dy, dz).getBlock();
                    if (!Tag.CROPS.isTagged(block.getType())) {
                        continue;
                    }
                    EntityChangeBlockEvent event =
                            new EntityChangeBlockEvent(mob, block, Material.AIR.createBlockData());
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        continue;
                    }
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5D, 0.3D, 0.5D),
                            12, 0.3D, 0.2D, 0.3D, block.getBlockData());
                    block.setType(Material.AIR, false);
                    ruined++;
                }
            }
        }
    }

    /** Whether this world lets mobs change blocks at all. */
    private static boolean mayGrief(World world) {
        return Boolean.TRUE.equals(world.getGameRuleValue(GameRule.MOB_GRIEFING));
    }

    // ------------------------------------------------------------------
    // Targets
    // ------------------------------------------------------------------

    /**
     * What a raid goes for, in the order it goes for it: the golem that would
     * stop it, then the people it came for.
     */
    private static LivingEntity findRaidTarget(Mob mob) {
        LivingEntity golem = null;
        LivingEntity villager = null;
        double bestGolem = Double.MAX_VALUE;
        double bestVillager = Double.MAX_VALUE;
        for (Entity nearby : mob.getNearbyEntities(VILLAGE_RADIUS, VILLAGE_RADIUS / 2, VILLAGE_RADIUS)) {
            if (!(nearby instanceof LivingEntity living) || living.isDead()) {
                continue;
            }
            double distance = living.getLocation().distanceSquared(mob.getLocation());
            if (living instanceof IronGolem && distance < bestGolem) {
                bestGolem = distance;
                golem = living;
            } else if (living instanceof AbstractVillager && distance < bestVillager) {
                bestVillager = distance;
                villager = living;
            }
        }
        return golem != null ? golem : villager;
    }

    /**
     * Something harmless to break the boredom on.
     *
     * <p>Babies are deliberately not filtered out - a herd is not spared because
     * half of it is young, and it is the calves scattering that actually reads
     * as something having gone wrong out there. Tamed animals are included for
     * the same reason: a boss walking through somebody's farm is supposed to be
     * a problem.</p>
     */
    private static LivingEntity findPrey(Mob mob) {
        List<LivingEntity> candidates = new ArrayList<>();
        for (Entity nearby : mob.getNearbyEntities(PREY_RADIUS, PREY_RADIUS / 2, PREY_RADIUS)) {
            if (nearby instanceof LivingEntity living && !living.isDead()
                    && (living instanceof Animals || living instanceof AbstractVillager)) {
                candidates.add(living);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /**
     * The boss's own swing, applied by the plugin rather than left to vanilla.
     *
     * <p>The same call the guardian call already makes, and for the same reason:
     * a zombie has an attack goal for players, villagers and golems, and none at
     * all for a chicken. Waiting for vanilla to do it would mean the livestock
     * half of this simply never happened.</p>
     */
    private static void strike(Mob mob, BossState state, LivingEntity quarry, long now) {
        if (now - state.lastAttackMillis() < MELEE_INTERVAL_MILLIS) {
            return;
        }
        if (mob.getLocation().distanceSquared(quarry.getLocation()) > STRIKE_RANGE * STRIKE_RANGE) {
            return;
        }
        state.markAttacked(now);
        AttributeInstance damage = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        quarry.damage(damage == null ? 6.0D : damage.getValue(), mob);
    }

    private static boolean isRangedArmed(Mob mob) {
        if (mob.getEquipment() == null) {
            return false;
        }
        Material held = mob.getEquipment().getItemInMainHand().getType();
        return held == Material.BOW || held == Material.CROSSBOW;
    }

    private static double healthRatio(Mob mob) {
        AttributeInstance maximum = mob.getAttribute(Attribute.MAX_HEALTH);
        return maximum == null || maximum.getValue() <= 0 ? 1.0D : mob.getHealth() / maximum.getValue();
    }

    /**
     * The closest player within {@code radius}, or {@code null} when nobody is.
     *
     * <p>Creative and spectator players are invisible to a boss on purpose: a
     * staff member flying out to look at one must not make it flee, stop
     * raiding, or spend its escapes on somebody who was never in danger.</p>
     */
    static Player nearestPlayer(Mob mob, double radius) {
        Player nearest = null;
        double best = radius * radius;
        for (Player player : mob.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode() == GameMode.CREATIVE
                    || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * Moves the boss {@code distance} blocks away from - or toward - the nearest
     * player, landing on solid ground. A blink that finds nowhere to land simply
     * does not happen, which is much better than dropping a boss into a wall.
     */
    private static void blink(Mob mob, int distance, boolean away) {
        Player nearest = nearestPlayer(mob, HUNT_RADIUS);
        if (nearest == null) {
            return;
        }
        Vector direction = nearest.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (direction.lengthSquared() == 0) {
            return;
        }
        direction = direction.normalize().multiply(away ? -distance : distance);
        World world = mob.getWorld();
        int x = mob.getLocation().getBlockX() + direction.getBlockX();
        int z = mob.getLocation().getBlockZ() + direction.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() || !world.getBlockAt(x, y + 1, z).getType().isAir()) {
            return;
        }
        Location destination = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
        world.spawnParticle(Particle.PORTAL, mob.getLocation().add(0, 1, 0), 30, 0.4D, 0.8D, 0.4D, 0.4D);
        mob.teleportAsync(destination);
    }
}
