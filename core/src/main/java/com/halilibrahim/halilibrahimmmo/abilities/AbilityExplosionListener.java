package com.halilibrahim.halilibrahimmmo.abilities;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Disarms a Maden Bombası and runs the plugin's own sweep instead.
 *
 * <p>The charge is a real primed TNT because that is the only thing that
 * <em>looks</em> and <em>sounds</em> like a charge to everybody standing near
 * it - the fuse, the flashing, the hiss. Its explosion, though, is exactly the
 * wrong shape for a mining tool: a vanilla blast hurts the miner who set it,
 * kills anything nearby, sets fire to what it does not destroy, and shatters
 * the ore the ability exists to uncover. So the explosion is reduced to
 * nothing at prime time and the interesting part is done by
 * {@link AbilityService#detonateMineBomb}, under rules this plugin controls.</p>
 *
 * <p>Only charges carrying this plugin's own tag are touched. Ordinary TNT, a
 * creeper and a bed in the Nether all behave exactly as they always did - the
 * listener is written as a tag check first and nothing else, so there is no
 * way for it to quietly change vanilla behaviour anywhere.</p>
 */
public final class AbilityExplosionListener implements Listener {

    private final AbilityService abilities;
    private final NamespacedKey radiusKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey abilityKey;

    public AbilityExplosionListener(
            AbilityService abilities, NamespacedKey radiusKey, NamespacedKey ownerKey, NamespacedKey abilityKey
    ) {
        this.abilities = abilities;
        this.radiusKey = radiusKey;
        this.ownerKey = ownerKey;
        this.abilityKey = abilityKey;
    }

    /**
     * A radius of zero is what actually makes the charge harmless: the server
     * never computes a blast at all, so no block is queued for destruction and
     * no entity is queued for damage. Cancelling {@link EntityExplodeEvent}
     * later would be too late for the damage half.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrime(ExplosionPrimeEvent event) {
        if (!isMineBomb(event.getEntity())) {
            return;
        }
        event.setRadius(0f);
        event.setFire(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!isMineBomb(entity)) {
            return;
        }
        // Belt and braces: with radius zero this list is already empty, but a
        // future plugin raising the radius back must not be able to make our
        // charge destroy anything.
        event.blockList().clear();
        Integer radius = entity.getPersistentDataContainer().get(radiusKey, PersistentDataType.INTEGER);
        String ownerId = entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        String abilityId = entity.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if (radius == null || ownerId == null || abilityId == null) {
            return;
        }
        abilities.detonateMineBomb(entity.getLocation(), UUID.fromString(ownerId), abilityId, radius);
    }

    /**
     * Nothing a mine bomb does may ever hurt anybody. Belongs here rather than
     * in the sweep because the damage would arrive from the vanilla explosion,
     * which resolves before the sweep ever starts.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlastDamage(EntityDamageByEntityEvent event) {
        if (isMineBomb(event.getDamager())) {
            event.setCancelled(true);
        }
    }

    private boolean isMineBomb(Entity entity) {
        return entity.getPersistentDataContainer().has(radiusKey, PersistentDataType.INTEGER);
    }
}
