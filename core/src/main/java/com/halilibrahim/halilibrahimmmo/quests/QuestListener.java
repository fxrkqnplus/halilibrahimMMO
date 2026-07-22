package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.classes.ClassExperienceAwardResult;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.exploit.BlockBreakRateLimiter;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Bridges valid player actions into persistent quest progress, XP rewards and the adventure journal. */
public final class QuestListener implements Listener {

    private final JavaPlugin plugin;
    private final QuestService quests;
    private final AbilityService abilities;
    private final ProfileService profiles;
    private final ClassService classes;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;
    private final JournalService journal;
    private final BlockBreakRateLimiter breakRateLimiter;
    private final SidebarService sidebar;
    private final ActivationService activation;

    public QuestListener(
            JavaPlugin plugin,
            QuestService quests,
            AbilityService abilities,
            ProfileService profiles,
            ClassService classes,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            MessageService messages,
            JournalService journal,
            BlockBreakRateLimiter breakRateLimiter,
            SidebarService sidebar,
            ActivationService activation
    ) {
        this.plugin = plugin;
        this.quests = quests;
        this.abilities = abilities;
        this.profiles = profiles;
        this.classes = classes;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
        this.journal = journal;
        this.breakRateLimiter = breakRateLimiter;
        this.sidebar = sidebar;
        this.activation = activation;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        quests.loadAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        quests.evict(event.getPlayer().getUniqueId());
        breakRateLimiter.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!activation.cached(player.getUniqueId()) || abilities.isSecondaryBreak(event) || !isGameplayGameMode(player)) {
            return;
        }
        if (!breakRateLimiter.allow(player.getUniqueId())) {
            return;
        }
        quests.awardBlock(player.getUniqueId(), event.getBlock().getType())
                .thenAccept(completed -> notify(player, completed));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntity() instanceof Player || !activation.cached(killer.getUniqueId())
                || !isGameplayGameMode(killer)) {
            return;
        }
        quests.awardMob(killer.getUniqueId(), event.getEntity().getType())
                .thenAccept(completed -> notify(killer, completed));
    }

    /** Creative/Spectator players never progress quests - no risk, no reward, matches skill/class XP gating. */
    private boolean isGameplayGameMode(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    private void notify(Player player, List<QuestProgress> completed) {
        completed.forEach(progress -> {
            QuestDefinition definition = quests.settings().definitions().get(progress.questId());
            player.getScheduler().run(plugin, task -> {
                messages.send(player, MessageKey.QUEST_COMPLETED, Map.of(
                        "gorev", definition.displayName(),
                        "xp", Long.toString(definition.rewardExperience())
                ));
                feedback.celebrateQuestCompleted(player, definition.displayName());
            }, null);
            if (definition.type() == QuestType.DAILY || definition.type() == QuestType.WEEKLY) {
                messages.broadcast(MessageKey.QUEST_COMPLETION_BROADCAST, Map.of("oyuncu", player.getName(), "gorev", definition.displayName()));
            }
            String classId = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
            if (!classId.isEmpty()) {
                classes.awardClassExperience(player.getUniqueId(), player.getName(), classId, definition.rewardExperience())
                        .thenAccept(result -> showExperienceProgress(player, classId, definition.rewardExperience(), result));
            }
            journal.recordQuestCompleted(player.getUniqueId(), definition.id(), definition.displayName(), definition.rewardExperience());
            titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
            cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
        });
    }

    private void showExperienceProgress(Player player, String classId, long amount, ClassExperienceAwardResult result) {
        if (amount <= 0) {
            return;
        }
        var classDefinition = classes.settings().definitions().get(classId);
        if (classDefinition == null) {
            return;
        }
        var classProgress = classes.classProgress(player.getUniqueId(), classId);
        long requiredForLevel = classProgress.level() >= classes.settings().classLevelCurve().maximumLevel()
                ? 0L
                : classes.settings().classLevelCurve().requiredForCurrentLevel(classProgress.level());
        player.getScheduler().run(plugin, task -> {
            feedback.showExperienceProgress(player, MessageKey.FEEDBACK_XP_CLASS_BOSSBAR, Map.of(
                    "sinif", classDefinition.displayName(), "kazanilan", Long.toString(amount)
            ), classProgress.experience(), requiredForLevel, "class", classDefinition.barColor().color());
            if (result.leveledUp()) {
                feedback.celebrateClassLevelUp(player, classDefinition.displayName(), result.newLevel());
                notifyUnlockedAbilities(player, classId, result.previousLevel(), result.newLevel());
            }
            sidebar.refresh(player);
        }, null);
    }

    private void notifyUnlockedAbilities(Player player, String classId, int previousLevel, int newLevel) {
        for (var ability : abilities.definitions()) {
            if (!ability.classId().equals(classId)) {
                continue;
            }
            if (ability.requiredLevel() > previousLevel && ability.requiredLevel() <= newLevel) {
                feedback.celebrateAbilityUnlocked(player, ability.displayName());
            }
        }
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
