package com.halilibrahim.halilibrahimmmo.feedback;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Refuses the damage from a celebration firework this plugin set off.
 *
 * <p>A vanilla firework star hurts everything within a couple of blocks of the
 * burst. Outdoors that is nobody; inside the two-block corridor a player is
 * standing in when they finally prestige, it is the player being congratulated
 * - which is exactly where it was happening. Only charges carrying this
 * plugin's own tag are touched, so a firework somebody crafted, a rocket fired
 * from a crossbow and an elytra boost all behave exactly as they always did.</p>
 */
public final class FireworkDamageListener implements Listener {

    private final NamespacedKey harmlessKey;

    public FireworkDamageListener(NamespacedKey harmlessKey) {
        this.harmlessKey = harmlessKey;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager.getPersistentDataContainer().has(harmlessKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}
