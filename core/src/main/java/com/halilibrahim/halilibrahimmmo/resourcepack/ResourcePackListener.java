package com.halilibrahim.halilibrahimmmo.resourcepack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Bridges Bukkit's join/quit/pack-status events into {@link ResourcePackService}.
 * Holds no state of its own - the service owns every decision, the same shape
 * as {@code ModCheckListener}.
 */
public final class ResourcePackListener implements Listener {

    private final ResourcePackService resourcePack;

    public ResourcePackListener(ResourcePackService resourcePack) {
        this.resourcePack = resourcePack;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        resourcePack.begin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStatus(PlayerResourcePackStatusEvent event) {
        resourcePack.handleStatus(event.getPlayer(), event.getStatus());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        resourcePack.forget(event.getPlayer().getUniqueId());
    }
}
