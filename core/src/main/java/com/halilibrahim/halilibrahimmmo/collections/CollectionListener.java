package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.classes.ClassExperienceAwardResult;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/** Bridges item pickups into persistent collection entries, XP rewards and the adventure journal. */
public final class CollectionListener implements Listener {

    private final JavaPlugin plugin;
    private final CollectionService collections;
    private final ProfileService profiles;
    private final ClassService classes;
    private final AbilityService abilities;
    private final TitleService titles;
    private final CosmeticService cosmetics;
    private final FeedbackService feedback;
    private final MessageService messages;
    private final JournalService journal;
    private final SidebarService sidebar;
    private final ActivationService activation;

    public CollectionListener(
            JavaPlugin plugin,
            CollectionService collections,
            ProfileService profiles,
            ClassService classes,
            AbilityService abilities,
            TitleService titles,
            CosmeticService cosmetics,
            FeedbackService feedback,
            MessageService messages,
            JournalService journal,
            SidebarService sidebar,
            ActivationService activation
    ) {
        this.plugin = plugin;
        this.collections = collections;
        this.profiles = profiles;
        this.classes = classes;
        this.abilities = abilities;
        this.titles = titles;
        this.cosmetics = cosmetics;
        this.feedback = feedback;
        this.messages = messages;
        this.journal = journal;
        this.sidebar = sidebar;
        this.activation = activation;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        collections.loadAll(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        collections.evict(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !activation.cached(player.getUniqueId())) {
            return;
        }
        collections.awardPickup(player.getUniqueId(), event.getItem().getItemStack().getType())
                .thenAccept(awards -> notify(player, awards));
    }

    private void notify(Player player, List<CollectionEntryAward> awards) {
        String classId = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
        awards.forEach(award -> {
            player.getScheduler().run(plugin, task -> {
                messages.send(player, MessageKey.COLLECTION_ENTRY_OBTAINED, Map.of(
                        "oge", award.entryDisplayName(),
                        "koleksiyon", award.collectionDisplayName(),
                        "xp", Long.toString(award.entryExperience())
                ));
                if (award.collectionCompleted()) {
                    messages.send(player, MessageKey.COLLECTION_COMPLETED, Map.of(
                            "koleksiyon", award.collectionDisplayName(),
                            "xp", Long.toString(award.completionExperience())
                    ));
                    feedback.celebrateCollectionCompleted(player, award.collectionDisplayName());
                }
            }, null);
            if (!classId.isEmpty()) {
                classes.awardClassExperience(player.getUniqueId(), player.getName(), classId, award.entryExperience())
                        .thenAccept(result -> showExperienceProgress(player, classId, award.entryExperience(), result));
            }
            journal.recordCollectionEntryObtained(player.getUniqueId(), award.entryId(), award.entryDisplayName(), award.collectionDisplayName(), award.entryExperience());
            if (award.collectionCompleted()) {
                if (!classId.isEmpty()) {
                    classes.awardClassExperience(player.getUniqueId(), player.getName(), classId, award.completionExperience())
                            .thenAccept(result -> showExperienceProgress(player, classId, award.completionExperience(), result));
                }
                journal.recordCollectionCompleted(player.getUniqueId(), award.collectionId(), award.collectionDisplayName(), award.completionExperience());
                titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
            }
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
