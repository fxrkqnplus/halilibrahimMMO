package com.halilibrahim.halilibrahimmmo.cosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Warms/evicts the equipped-cosmetic cache on player lifecycle and celebrates on join. */
public final class CosmeticListener implements Listener {

    private final JavaPlugin plugin;
    private final CosmeticService cosmetics;

    public CosmeticListener(JavaPlugin plugin, CosmeticService cosmetics) {
        this.plugin = plugin;
        this.cosmetics = cosmetics;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cosmetics.findEquipped(player.getUniqueId()).thenAccept(ignored ->
                player.getScheduler().run(plugin, task -> cosmetics.celebrate(player, CelebrationMoment.JOIN), null));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cosmetics.evict(event.getPlayer().getUniqueId());
    }
}
