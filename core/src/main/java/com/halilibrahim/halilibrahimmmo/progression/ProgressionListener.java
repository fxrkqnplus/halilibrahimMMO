package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.prestige.BankedPowerService;
import com.halilibrahim.halilibrahimmmo.prestige.PrestigeRewardService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.reputation.ReputationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Warms and evicts per-player caches on join/quit. Güç has no independent XP
 * source of its own anymore (it is purely derived, see {@link ProgressionService}) -
 * block/mob/playtime rewards now flow entirely through the per-skill
 * ({@code skills.SkillListener}) path, so this listener no longer awards
 * anything itself.
 */
public final class ProgressionListener implements Listener {

    private final ProfileService profiles;
    private final FeedbackService feedback;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final ActivationService activation;
    private final BankedPowerService bankedPower;
    private final PrestigeRewardService prestigeRewards;
    private final ReputationService reputation;

    public ProgressionListener(
            ProfileService profiles,
            FeedbackService feedback,
            BlockBreakRateLimiter breakRateLimiter,
            ActivationService activation,
            BankedPowerService bankedPower,
            PrestigeRewardService prestigeRewards,
            ReputationService reputation
    ) {
        this.profiles = profiles;
        this.feedback = feedback;
        this.breakRateLimiter = breakRateLimiter;
        this.activation = activation;
        this.bankedPower = bankedPower;
        this.prestigeRewards = prestigeRewards;
        this.reputation = reputation;
    }

    public void close() {
        // No shared resources to release - see SkillListener's playtime task for the actual repeating award.
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profiles.load(player.getUniqueId(), player.getName());
        activation.isActivated(player.getUniqueId());
        bankedPower.load(player.getUniqueId());
        // Warmed here rather than applied here: the health half of a prestige
        // reward is written on by ClassListener once the class effects it has to
        // coexist with have been applied.
        prestigeRewards.load(player.getUniqueId());
        reputation.load(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.evict(event.getPlayer().getUniqueId());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
        feedback.clearExperienceProgress(event.getPlayer());
        activation.evict(event.getPlayer().getUniqueId());
        bankedPower.evict(event.getPlayer().getUniqueId());
        prestigeRewards.evict(event.getPlayer().getUniqueId());
        reputation.evict(event.getPlayer().getUniqueId());
    }
}
