package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.api.progression.ProgressionCause;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CelebrationMoment;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.List;
import java.util.Map;

/** Bridges valid Paper events into the centralized profile and XP paths. */
public final class ProgressionListener implements Listener {

    private final JavaPlugin plugin;
    private final ProfileService profiles;
    private final ProgressionService progression;
    private final AbilityService abilities;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final SidebarService sidebar;
    private final ActivationService activation;
    private ScheduledTask playtimeTask;

    public ProgressionListener(
            JavaPlugin plugin,
            ProfileService profiles,
            ProgressionService progression,
            AbilityService abilities,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            MessageService messages,
            BlockBreakRateLimiter breakRateLimiter,
            SidebarService sidebar,
            ActivationService activation
    ) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.progression = progression;
        this.abilities = abilities;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
        this.breakRateLimiter = breakRateLimiter;
        this.sidebar = sidebar;
        this.activation = activation;
        this.playtimeTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task ->
                Bukkit.getOnlinePlayers().stream().filter(player -> activation.cached(player.getUniqueId()))
                        .forEach(player -> award(player, ProgressionCause.PLAYTIME, "playtime_minutes")),
                20L * 60L, 20L * 60L);
    }

    public void close() {
        if (playtimeTask != null) {
            playtimeTask.cancel();
            playtimeTask = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        profiles.load(player.getUniqueId(), player.getName());
        activation.isActivated(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.evict(event.getPlayer().getUniqueId());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
        feedback.clearExperienceProgress(event.getPlayer());
        activation.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (abilities.isSecondaryBreak(event)) {
            return;
        }
        if (!breakRateLimiter.allow(event.getPlayer().getUniqueId())) {
            return;
        }
        award(event.getPlayer(), ProgressionCause.BLOCK_BREAK, "blocks_broken");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            award(killer, ProgressionCause.MOB_KILL, "mobs_killed");
        }
    }

    private void award(Player player, ProgressionCause cause, String statisticKey) {
        if (!activation.cached(player.getUniqueId())) {
            return;
        }
        long amount = progression.settings().rewardFor(cause);
        progression.awardConfigured(player.getUniqueId(), player.getName(), cause).thenAccept(result -> {
            profiles.incrementStatistic(player.getUniqueId(), statisticKey, 1);
            player.getScheduler().run(plugin, task -> {
                sidebar.refresh(player);
                showExperienceProgress(player, cause, amount, result);
            }, null);
            if (!result.leveledUp()) {
                return;
            }
            int newLevel = result.snapshot().globalLevel();
            player.getScheduler().run(plugin, task -> {
                feedback.celebrateLevelUp(player, newLevel);
                cosmetics.celebrate(player, CelebrationMoment.LEVEL_UP);
            }, null);
            titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
            cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
        });
    }

    private void showExperienceProgress(Player player, ProgressionCause cause, long amount, ProgressionAwardResult result) {
        if (amount <= 0) {
            return;
        }
        MessageKey template = switch (cause) {
            case BLOCK_BREAK -> MessageKey.FEEDBACK_XP_BLOCK_BREAK_BOSSBAR;
            case MOB_KILL -> MessageKey.FEEDBACK_XP_MOB_KILL_BOSSBAR;
            case PLAYTIME -> MessageKey.FEEDBACK_XP_PLAYTIME_BOSSBAR;
            case QUEST, COLLECTION, EXTERNAL -> null;
        };
        if (template == null) {
            return;
        }
        int level = result.snapshot().globalLevel();
        long requiredForLevel = level >= progression.settings().curve().maximumLevel()
                ? 0L
                : progression.settings().curve().requiredForCurrentLevel(level);
        feedback.showExperienceProgress(player, template, Map.of("kazanilan", Long.toString(amount)),
                result.snapshot().globalExperience(), requiredForLevel, "global", BossBar.Color.GREEN);
    }

    private void notifyTitles(Player player, List<TitleDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.TITLE_UNLOCKED, Map.of("unvan", definition.displayName()))), null);
    }

    private void notifyCosmetics(Player player, List<CosmeticDefinition> unlocked) {
        if (unlocked.isEmpty()) {
            return;
        }
        player.getScheduler().run(plugin, task -> unlocked.forEach(definition ->
                messages.send(player, MessageKey.COSMETIC_UNLOCKED, Map.of("kozmetik", definition.displayName()))), null);
    }
}
