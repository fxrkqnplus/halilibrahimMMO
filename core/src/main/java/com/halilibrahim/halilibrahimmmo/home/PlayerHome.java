package com.halilibrahim.halilibrahimmmo.home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;
import java.util.UUID;

/**
 * One player's named home.
 *
 * <p>Stored as a world UUID plus raw coordinates rather than a Bukkit
 * {@link Location}, because a {@code Location} holds a live {@link World}
 * reference that cannot survive a database round-trip or a world that has not
 * been loaded yet.</p>
 */
public record PlayerHome(
        UUID playerId, String name, UUID worldId, double x, double y, double z, float yaw, float pitch
) {

    public static PlayerHome of(UUID playerId, String name, Location location) {
        return new PlayerHome(playerId, name, location.getWorld().getUID(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /** Empty when the home's world is no longer loaded on this server. */
    public Optional<Location> toLocation() {
        World world = Bukkit.getWorld(worldId);
        return world == null ? Optional.empty() : Optional.of(new Location(world, x, y, z, yaw, pitch));
    }
}
