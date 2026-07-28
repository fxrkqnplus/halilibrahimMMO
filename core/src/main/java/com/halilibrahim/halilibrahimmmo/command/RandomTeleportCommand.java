package com.halilibrahim.halilibrahimmmo.command;

import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.DurationFormatter;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.util.PlayerCooldowns;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /rtp}: drops the player somewhere random and habitable in the
 * overworld, once every {@value #COOLDOWN_MINUTES} minutes.
 *
 * <p><strong>The search is asynchronous by necessity, not by preference.</strong>
 * A random point ten thousand blocks away is almost never in a loaded chunk, and
 * asking for its highest block would generate that chunk on the spot - a visible
 * server freeze for everyone. Each candidate is loaded through
 * {@link World#getChunkAtAsync(int, int)} first and only then inspected, and a
 * candidate that turns out to be ocean, lava or a cave ceiling simply costs
 * another round of the same loop rather than a bad landing.</p>
 *
 * <p>Overworld only. The Nether has a roof that every "highest block" answer
 * lands on top of, and a random point in the End is either the void or a
 * dragon's island - neither is a place to put someone who typed one word.</p>
 */
public final class RandomTeleportCommand implements BasicCommand {

    /** Long enough that {@code /rtp} is an expedition rather than a way to travel. */
    private static final int COOLDOWN_MINUTES = 30;
    /** Far enough from spawn that the destination is genuinely somewhere else. */
    private static final int MINIMUM_DISTANCE = 500;
    private static final int MAXIMUM_DISTANCE = 10_000;
    /**
     * How many candidate points are tried before giving up and telling the
     * player to try again. Bounded because each attempt loads a chunk, and an
     * ocean-heavy world could otherwise keep the loop running indefinitely.
     */
    private static final int MAXIMUM_ATTEMPTS = 16;

    /** Solid ground that is still no place to land. */
    private static final Set<Material> UNSAFE_GROUND = Set.of(
            Material.LAVA, Material.WATER, Material.MAGMA_BLOCK, Material.CACTUS, Material.POWDER_SNOW,
            Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.SWEET_BERRY_BUSH
    );

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final FeedbackService feedback;
    private final PlayerCooldowns cooldowns = new PlayerCooldowns(COOLDOWN_MINUTES * 60L * 1_000L);

    public RandomTeleportCommand(JavaPlugin plugin, MessageService messages, FeedbackService feedback) {
        this.plugin = plugin;
        this.messages = messages;
        this.feedback = feedback;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (!(stack.getSender() instanceof Player player)) {
            messages.send(stack.getSender(), MessageKey.PLAYER_ONLY);
            return;
        }
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            messages.send(player, MessageKey.RANDOM_TELEPORT_FORBIDDEN_WORLD);
            return;
        }
        long remaining = cooldowns.remainingMillis(player.getUniqueId());
        if (remaining > 0) {
            messages.send(player, MessageKey.RANDOM_TELEPORT_COOLDOWN, Map.of(
                    "sure", DurationFormatter.ofMillis(messages, remaining)
            ));
            return;
        }
        // Claimed before the search, not after it: the search takes several
        // chunk loads, and a player spamming the command while one is running
        // would otherwise start a dozen of them at once.
        cooldowns.start(player.getUniqueId());
        messages.send(player, MessageKey.RANDOM_TELEPORT_SEARCHING);
        attempt(player, world, 1);
    }

    private void attempt(Player player, World world, int attempt) {
        if (attempt > MAXIMUM_ATTEMPTS) {
            // Nothing was found, so the player keeps their turn.
            cooldowns.evict(player.getUniqueId());
            messages.send(player, MessageKey.RANDOM_TELEPORT_FAILED);
            return;
        }
        Location spawn = world.getSpawnLocation();
        int x = spawn.getBlockX() + signedOffset();
        int z = spawn.getBlockZ() + signedOffset();
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location destination = safeLandingAt(world, x, z);
            if (destination == null) {
                attempt(player, world, attempt + 1);
                return;
            }
            player.getScheduler().run(plugin, task -> arrive(player, destination), null);
        });
    }

    private void arrive(Player player, Location destination) {
        player.teleportAsync(destination).thenAccept(teleported -> player.getScheduler().run(plugin, task -> {
            if (!teleported) {
                cooldowns.evict(player.getUniqueId());
                messages.send(player, MessageKey.RANDOM_TELEPORT_FAILED);
                return;
            }
            String x = Integer.toString(destination.getBlockX());
            String y = Integer.toString(destination.getBlockY());
            String z = Integer.toString(destination.getBlockZ());
            feedback.celebrateRandomTeleport(player, x, y, z);
            messages.send(player, MessageKey.RANDOM_TELEPORT_ARRIVED, Map.of("x", x, "y", y, "z", z));
        }, null));
    }

    /** A random distance in {@code [MINIMUM_DISTANCE, MAXIMUM_DISTANCE]}, either side of spawn. */
    private int signedOffset() {
        int distance = ThreadLocalRandom.current().nextInt(MINIMUM_DISTANCE, MAXIMUM_DISTANCE + 1);
        return ThreadLocalRandom.current().nextBoolean() ? distance : -distance;
    }

    /**
     * The standing position on top of the highest block at {@code (x, z)}, or
     * {@code null} if that column is not somewhere a player can stand: open sky
     * over water or lava, a fire, a cactus, or anything without two blocks of
     * clear air above it.
     */
    private Location safeLandingAt(World world, int x, int z) {
        int groundY = world.getHighestBlockYAt(x, z);
        if (groundY <= world.getMinHeight()) {
            return null;
        }
        Block ground = world.getBlockAt(x, groundY, z);
        if (!ground.getType().isSolid() || UNSAFE_GROUND.contains(ground.getType())) {
            return null;
        }
        if (!world.getBlockAt(x, groundY + 1, z).getType().isAir()
                || !world.getBlockAt(x, groundY + 2, z).getType().isAir()) {
            return null;
        }
        return new Location(world, x + 0.5D, groundY + 1.0D, z + 0.5D);
    }
}
