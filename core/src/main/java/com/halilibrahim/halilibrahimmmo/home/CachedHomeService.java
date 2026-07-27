package com.halilibrahim.halilibrahimmmo.home;

import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Write-through, join-warmed cache over {@link HomeRepository}. */
public final class CachedHomeService implements HomeService {

    private final JavaPlugin plugin;
    private final HomeRepository repository;
    private final ProfileService profiles;
    private final ClassService classes;
    private final ConcurrentHashMap<UUID, Optional<PlayerHome>> cache = new ConcurrentHashMap<>();

    public CachedHomeService(JavaPlugin plugin, HomeRepository repository, ProfileService profiles, ClassService classes) {
        this.plugin = plugin;
        this.repository = repository;
        this.profiles = profiles;
        this.classes = classes;
    }

    @Override
    public void enable() {
        // No YAML settings - reload() stays the PluginService default no-op.
    }

    @Override
    public void disable() {
        cache.clear();
    }

    @Override
    public Optional<PlayerHome> cached(UUID playerId) {
        return cache.getOrDefault(playerId, Optional.empty());
    }

    @Override
    public boolean isUnlocked(UUID playerId) {
        return profiles.cached(playerId).map(this::isUnlocked).orElse(false);
    }

    private boolean isUnlocked(PlayerProfile profile) {
        String classId = profile.selectedClass();
        int classLevel = classId.isEmpty() ? 0 : classes.classProgress(profile.playerId(), classId).level();
        return FeatureUnlockRules.isUnlocked(classLevel, profile.prestige(), FeatureUnlockRules.HOME_CLASS_LEVEL);
    }

    @Override
    public CompletableFuture<Optional<PlayerHome>> load(UUID playerId) {
        Optional<PlayerHome> cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return repository.find(playerId).thenApply(found -> {
            cache.put(playerId, found);
            return found;
        });
    }

    @Override
    public CompletableFuture<HomeActionResult> setHome(Player player, String name) {
        if (!isUnlocked(player.getUniqueId())) {
            return CompletableFuture.completedFuture(HomeActionResult.LOCKED);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAXIMUM_NAME_LENGTH) {
            return CompletableFuture.completedFuture(HomeActionResult.INVALID_NAME);
        }
        Location location = player.getLocation();
        if (isForbidden(location.getWorld())) {
            return CompletableFuture.completedFuture(HomeActionResult.FORBIDDEN_WORLD);
        }
        PlayerHome home = PlayerHome.of(player.getUniqueId(), trimmed, location);
        return repository.save(home).thenApply(ignored -> {
            cache.put(player.getUniqueId(), Optional.of(home));
            return HomeActionResult.SUCCESS;
        });
    }

    @Override
    public CompletableFuture<HomeActionResult> teleportHome(Player player) {
        if (!isUnlocked(player.getUniqueId())) {
            return CompletableFuture.completedFuture(HomeActionResult.LOCKED);
        }
        // Checked before the lookup: leaving the Nether or the End by command is
        // the thing being prevented, whether or not a home even exists.
        if (isForbidden(player.getWorld())) {
            return CompletableFuture.completedFuture(HomeActionResult.FORBIDDEN_WORLD);
        }
        return load(player.getUniqueId()).thenApply(found -> {
            if (found.isEmpty()) {
                return HomeActionResult.NOT_SET;
            }
            Optional<Location> destination = found.get().toLocation();
            if (destination.isEmpty()) {
                return HomeActionResult.WORLD_MISSING;
            }
            // The lookup resolved on a database thread; an entity may only be
            // moved from the region that owns it.
            player.getScheduler().run(plugin, task -> player.teleportAsync(destination.get()), null);
            return HomeActionResult.SUCCESS;
        });
    }

    @Override
    public CompletableFuture<HomeActionResult> deleteHome(Player player) {
        return load(player.getUniqueId()).thenCompose(found -> {
            if (found.isEmpty()) {
                return CompletableFuture.completedFuture(HomeActionResult.NOT_SET);
            }
            return repository.delete(player.getUniqueId()).thenApply(ignored -> {
                cache.put(player.getUniqueId(), Optional.empty());
                return HomeActionResult.SUCCESS;
            });
        });
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
    }

    private boolean isForbidden(World world) {
        return world.getEnvironment() != World.Environment.NORMAL;
    }
}
