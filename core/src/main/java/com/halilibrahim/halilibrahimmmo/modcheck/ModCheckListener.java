package com.halilibrahim.halilibrahimmmo.modcheck;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Bridges Bukkit's join/quit events and the {@code hmmo:modcheck} channel into
 * {@link ModVerificationService}. Holds no state of its own - the service owns
 * every decision, so the command surface (none) and this listener can never
 * drift apart.
 */
public final class ModCheckListener implements Listener, PluginMessageListener {

    private final ModVerificationService verification;

    public ModCheckListener(ModVerificationService verification) {
        this.verification = verification;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        verification.begin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        verification.forget(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!ModCheckProtocol.CHANNEL.equals(channel)) {
            return;
        }
        verification.handleResponse(player, message);
    }
}
