package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Applies selected-class passives on player lifecycle and valid PvE damage.
 *
 * <p>Also the one registered listener for {@link EntityDamageByEntityEvent}
 * against a valid non-player living entity, so it forwards to
 * {@link AbilityService#handleEntityDamage} for the {@code DAMAGE_BUFF}
 * ability effect (e.g. Avcı Odağı) - that effect type existed since Phase 3
 * but had no event source wired to it until this phase.</p>
 */
public final class ClassListener implements Listener {
    private final JavaPlugin plugin; private final ClassService classes; private final AbilityService abilities; private final EnergyService energy;
    public ClassListener(JavaPlugin plugin, ClassService classes, AbilityService abilities, EnergyService energy) {
        this.plugin = plugin; this.classes = classes; this.abilities = abilities; this.energy = energy;
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        classes.load(player.getUniqueId(), player.getName()).thenAccept(ignored -> player.getScheduler().run(plugin, task -> classes.applyPlayerEffects(player), null));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { classes.evict(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) return;
        event.setDamage(event.getDamage() * (1.0D + classes.modifiers(player.getUniqueId()).pveDamageBonus()));
        abilities.handleEntityDamage(event, energy);
    }
}
