package com.halilibrahim.halilibrahimmmo.scoreboard;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.onboarding.OnboardingService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shows the sidebar once a joining player's profile, class and activation
 * state are all warmed in their respective caches, and clears its in-memory
 * visibility state on quit. {@link ProfileService#load}/{@link ClassService#load}
 * are safe to call again here even though {@code ProgressionListener}/
 * {@code ClassListener} already load the same data on join - both simply
 * resolve from cache once loaded. Waiting on {@link ActivationService#isActivated}
 * here specifically matters: rendering before it resolves would default to
 * the "not activated" prompt line even for an already-activated returning
 * player, with nothing to self-correct it afterward.
 */
public final class SidebarListener implements Listener {

    private final JavaPlugin plugin;
    private final SidebarService sidebar;
    private final ProfileService profiles;
    private final ClassService classes;
    private final ActivationService activation;
    private final OnboardingService onboarding;

    public SidebarListener(
            JavaPlugin plugin, SidebarService sidebar, ProfileService profiles,
            ClassService classes, ActivationService activation, OnboardingService onboarding
    ) {
        this.plugin = plugin;
        this.sidebar = sidebar;
        this.profiles = profiles;
        this.classes = classes;
        this.activation = activation;
        this.onboarding = onboarding;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profiles.load(player.getUniqueId(), player.getName())
                .thenCompose(profile -> classes.load(player.getUniqueId(), player.getName()).thenApply(ignored -> profile))
                .thenCompose(profile -> activation.isActivated(player.getUniqueId()).thenApply(ignored -> profile))
                .thenCompose(profile -> onboarding.loadCompleted(player.getUniqueId()).thenApply(ignored -> profile))
                .thenAccept(ignored -> player.getScheduler().run(plugin, task -> sidebar.initialize(player), null));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sidebar.remove(event.getPlayer());
        onboarding.evict(event.getPlayer().getUniqueId());
    }
}
