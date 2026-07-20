package com.halilibrahim.halilibrahimmmo.titles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Warms/evicts the equipped-title cache on player lifecycle. */
public final class TitleListener implements Listener {

    private final TitleService titles;

    public TitleListener(TitleService titles) {
        this.titles = titles;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        titles.findEquipped(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        titles.evict(event.getPlayer().getUniqueId());
    }
}
