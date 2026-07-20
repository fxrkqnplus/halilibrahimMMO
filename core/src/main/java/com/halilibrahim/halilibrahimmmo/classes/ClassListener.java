package com.halilibrahim.halilibrahimmmo.classes;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Applies selected-class passives on player lifecycle and valid PvE damage. */
public final class ClassListener implements Listener {
    private final JavaPlugin plugin; private final ClassService classes;
    public ClassListener(JavaPlugin plugin, ClassService classes) { this.plugin = plugin; this.classes = classes; }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        classes.load(player.getUniqueId(), player.getName()).thenAccept(ignored -> player.getScheduler().run(plugin, task -> classes.applyPlayerEffects(player), null));
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { classes.evict(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) return;
        event.setDamage(event.getDamage() * (1.0D + classes.modifiers(player.getUniqueId()).pveDamageBonus()));
    }
}
