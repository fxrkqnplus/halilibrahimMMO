package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * World bosses: one named, rarity-tiered creature spawned somewhere on the map
 * at a fixed interval, announced to everyone, and worth killing.
 *
 * <p>A boss is nothing but a heavily modified vanilla mob carrying a
 * {@code PersistentDataContainer} tag, which is what makes the whole system
 * survive a restart without any table of its own: the server saves the entity,
 * the tag rides along with it, and the reward path reads the tag rather than an
 * in-memory registry. The one thing a restart does cost is the flee/heal
 * behaviour loop, which is re-attached only for bosses spawned since - an old
 * boss keeps its stats, its name and its full reward, and simply fights like a
 * very strong ordinary mob.</p>
 */
public interface BossService extends PluginService {

    BossSettings settings();

    /** Whether this creature is a world boss - i.e. carries the boss tag. */
    boolean isBoss(LivingEntity entity);

    /** The rarity tag on this creature, or empty if it is not a boss. */
    Optional<BossRarity> rarityOf(LivingEntity entity);

    /**
     * Spawns one boss right now near a randomly chosen online player, ignoring
     * the interval timer. Used by the admin panel; returns empty when there is
     * nobody online to spawn near.
     */
    Optional<String> spawnNow();
}
