package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.energy.EnergyService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import net.kyori.adventure.bossbar.BossBar;
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
    private final ProfileService profiles;
    private final ActivationService activation;
    private final BlockBreakRateLimiter breakRateLimiter;

    public SkillListener(
            JavaPlugin plugin, SkillService skills, AbilityService abilities, EnergyService energy,
            FeedbackService feedback, ProfileService profiles, ActivationService activation, BlockBreakRateLimiter breakRateLimiter
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.abilities = abilities;
        this.energy = energy;
        this.feedback = feedback;
        this.profiles = profiles;
        this.activation = activation;
        this.breakRateLimiter = breakRateLimiter;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        skills.loadAll(player.getUniqueId());
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
                && breakRateLimiter.allow(player.getUniqueId())) {
            skills.awardBlock(player.getUniqueId(), player.getName(), event.getBlock().getType())
                    .thenAccept(result -> handleAward(player, result));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && !(event.getEntity() instanceof Player) && activation.cached(killer.getUniqueId())) {
            skills.awardMob(killer.getUniqueId(), killer.getName(), event.getEntity().getType())
                    .thenAccept(result -> handleAward(killer, result));
        }
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
                    Map.of("beceri", definition.displayName(), "kazanilan", Long.toString(award.grantedAmount()), "kaynak", award.sourceDisplayName()),
                    progress.experience(), requiredForLevel, "skill", BossBar.Color.GREEN
            );
            if (award.leveledUp()) {
                feedback.celebrateSkillLevelUp(player, definition.displayName(), progress.level());
                notifyUnlockedAbilities(player, progress.skillId(), award.previousLevel(), progress.level());
            }
        }, null);
    }

    private void notifyUnlockedAbilities(Player player, String skillId, int previousLevel, int newLevel) {
        String selectedClass = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
        if (selectedClass.isEmpty()) {
            return;
        }
        for (AbilityDefinition ability : abilities.definitions()) {
            if (!ability.skillId().equals(skillId) || !ability.classId().equals(selectedClass)) {
                continue;
            }
            if (ability.requiredLevel() > previousLevel && ability.requiredLevel() <= newLevel) {
                feedback.celebrateAbilityUnlocked(player, ability.displayName());
            }
        }
    }
}
