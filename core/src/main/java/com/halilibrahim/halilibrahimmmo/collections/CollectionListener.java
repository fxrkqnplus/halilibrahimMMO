package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.activation.ActivationService;
import com.halilibrahim.halilibrahimmmo.api.progression.ProgressionCause;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticDefinition;
import com.halilibrahim.halilibrahimmmo.cosmetics.CosmeticService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.journal.JournalService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionAwardResult;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import com.halilibrahim.halilibrahimmmo.titles.TitleDefinition;
import com.halilibrahim.halilibrahimmmo.titles.TitleService;
import net.kyori.adventure.bossbar.BossBar;
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
    private final ProgressionService progression;
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
            ProgressionService progression,
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
        this.progression = progression;
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
            progression.award(player.getUniqueId(), player.getName(), award.entryExperience(), ProgressionCause.COLLECTION)
                    .thenAccept(result -> showExperienceProgress(player, award.entryExperience(), result));
            journal.recordCollectionEntryObtained(player.getUniqueId(), award.entryId(), award.entryDisplayName(), award.collectionDisplayName(), award.entryExperience());
            if (award.collectionCompleted()) {
                progression.award(player.getUniqueId(), player.getName(), award.completionExperience(), ProgressionCause.COLLECTION)
                        .thenAccept(result -> showExperienceProgress(player, award.completionExperience(), result));
                journal.recordCollectionCompleted(player.getUniqueId(), award.collectionId(), award.collectionDisplayName(), award.completionExperience());
                titles.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyTitles(player, unlocked));
                cosmetics.evaluate(player.getUniqueId(), player.getName()).thenAccept(unlocked -> notifyCosmetics(player, unlocked));
            }
        });
    }

    private void showExperienceProgress(Player player, long amount, ProgressionAwardResult result) {
        if (amount <= 0) {
            return;
        }
        int level = result.snapshot().globalLevel();
        long requiredForLevel = level >= progression.settings().curve().maximumLevel()
                ? 0L
                : progression.settings().curve().requiredForCurrentLevel(level);
        player.getScheduler().run(plugin, task -> {
            feedback.showExperienceProgress(player, MessageKey.FEEDBACK_XP_COLLECTION_BOSSBAR, Map.of("kazanilan", Long.toString(amount)),
                    result.snapshot().globalExperience(), requiredForLevel, "global", BossBar.Color.GREEN);
            sidebar.refresh(player);
        }, null);
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
