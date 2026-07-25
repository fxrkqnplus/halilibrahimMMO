package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

/** Bridges valid player actions into persistent skills and ephemeral energy state. */
public final class SkillListener implements Listener {

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final AbilityService abilities;
    private final EnergyService energy;
    private final FeedbackService feedback;
    private final ActivationService activation;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final ProgressionService progression;
    private final MessageService messages;

    public SkillListener(
            JavaPlugin plugin, SkillService skills, AbilityService abilities, EnergyService energy,
            FeedbackService feedback, ActivationService activation, BlockBreakRateLimiter breakRateLimiter,
            ProgressionService progression, MessageService messages
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.abilities = abilities;
        this.energy = energy;
        this.feedback = feedback;
        this.activation = activation;
        this.breakRateLimiter = breakRateLimiter;
        this.progression = progression;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        skills.loadAll(player.getUniqueId())
                .thenAccept(ignored -> player.getScheduler().run(plugin, task -> skills.applyMilestoneEffects(player), null));
        energy.initialize(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        skills.evict(event.getPlayer().getUniqueId());
        energy.remove(event.getPlayer());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        abilities.handleBlockBreak(event);
        Player player = event.getPlayer();
        if (activation.cached(player.getUniqueId()) && !abilities.isSecondaryBreak(event)
                && isGameplayGameMode(player) && breakRateLimiter.allow(player.getUniqueId())) {
            skills.awardBlock(player.getUniqueId(), player.getName(), event.getBlock().getType())
                    .thenAccept(result -> handleAward(player, result));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player) && activation.cached(killer.getUniqueId())
                && isGameplayGameMode(killer)) {
            skills.awardMob(killer.getUniqueId(), killer.getName(), event.getEntity().getType())
                    .thenAccept(result -> handleAward(killer, result));
        }
    }

    /** Creative/Spectator players never earn skill, class or Güç experience - no risk, no reward. */
    private boolean isGameplayGameMode(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    private void handleAward(Player player, Optional<SkillAwardResult> result) {
        if (result.isEmpty()) {
            return;
        }
        SkillAwardResult award = result.get();
        SkillProgress progress = award.progress();
        SkillDefinition definition = skills.settings().definitions().get(progress.skillId());
        if (definition == null) {
            return;
        }
        long requiredForLevel = progress.level() >= skills.settings().curve().maximumLevel()
                ? 0L
                : skills.settings().curve().requiredForCurrentLevel(progress.level());
        player.getScheduler().run(plugin, task -> {
            feedback.showExperienceProgress(
                    player, MessageKey.FEEDBACK_XP_SKILL_BOSSBAR,
                    Map.of(
                            "beceri", definition.displayName(),
                            "seviye", Integer.toString(progress.level()),
                            "kazanilan", Long.toString(award.grantedAmount()),
                            "kaynak", award.sourceDisplayName()
                    ),
                    progress.experience(), requiredForLevel, "skill", BossBar.Color.GREEN
            );
            if (award.leveledUp()) {
                feedback.celebrateSkillLevelUp(player, definition.displayName(), progress.level());
                announceGucMilestones(player, progress.level() - award.previousLevel());
                skills.applyMilestoneEffects(player);
            }
        }, null);
    }

    /**
     * Güç only ever changes as a byproduct of a skill level-up (it has no
     * independent XP source, see {@code ProgressionService}) - {@code delta}
     * is exactly how much the leveled skill's level just increased, so
     * {@code previousGuc} can be derived from the already-updated
     * {@code newGuc} without a second read.
     */
    private void announceGucMilestones(Player player, int delta) {
        if (delta <= 0) {
            return;
        }
        int newGuc = progression.cachedGlobalLevel(player.getUniqueId());
        int previousGuc = newGuc - delta;
        if (crossedMultipleOf(previousGuc, newGuc, 10)) {
            feedback.celebrateGucMilestone(player, newGuc);
        }
        for (int milestone : new int[] {25, 50, 75, 100}) {
            if (previousGuc < milestone && newGuc >= milestone) {
                messages.broadcast(MessageKey.GUC_BROADCAST, Map.of("oyuncu", player.getName(), "guc", Integer.toString(newGuc)));
            }
        }
    }

    private boolean crossedMultipleOf(int previousValue, int newValue, int step) {
        return newValue / step > previousValue / step;
    }
}
