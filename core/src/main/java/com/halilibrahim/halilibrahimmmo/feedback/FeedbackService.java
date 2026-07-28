package com.halilibrahim.halilibrahimmmo.feedback;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Universal (not equip-based) bossbar/on-screen-title polish for every XP
 * award, plus the level-up/quest/collection/prestige moments. Every method is
 * synchronous - callers must already be on the player's own region
 * scheduler, exactly like the {@code messages.send(...)} calls they sit
 * alongside.
 */
public interface FeedbackService extends PluginService {

    FeedbackSettings settings();

    /**
     * Shows or updates one per-player, per-{@code category} XP progress bossbar
     * ({@code category} e.g. {@code "global"}/{@code "skill"}/{@code "class"} -
     * distinct categories render as separate, simultaneous bars, e.g. a
     * synergy-skill kill shows both the skill bar and the class bar at once,
     * each its own {@code color}). {@code template} is one of the
     * {@code FEEDBACK_XP_*_BOSSBAR} {@link MessageKey}s, already carrying
     * whatever source-specific placeholders it needs (e.g. {@code beceri} for
     * the skill variant, {@code kaynak} for the specific block/mob) in
     * {@code placeholders} - {@code kazanilan}/{@code mevcut}/{@code gereken}
     * are added automatically. Repeated calls for the same player/category
     * update that bar in place and reset its auto-hide timer, rather than
     * stacking a new bar.
     *
     * @param currentExperience the player's XP already earned within their current level.
     * @param requiredExperience the XP required to complete that level (0 at the level cap).
     */
    void showExperienceProgress(
            Player player, MessageKey template, Map<String, String> placeholders,
            long currentExperience, long requiredExperience, String category, BossBar.Color color
    );

    /** Clears a player's in-memory XP progress bar state (called on quit). */
    void clearExperienceProgress(Player player);

    /** Sends the invoking player a chat-only, non-broadcast level-up message. */
    void celebrateLevelUp(Player player, int newLevel);

    void celebrateQuestCompleted(Player player, String questDisplayName);

    void celebrateCollectionCompleted(Player player, String collectionDisplayName);

    void celebratePrestige(Player player, int newPrestige);

    /** Bossbar+title/subtitle fade, fired when a class-exclusive ability's skill-level threshold is crossed. */
    void celebrateAbilityUnlocked(Player player, String abilityDisplayName);

    /** Sends the invoking player a chat-only, non-broadcast class level-up message. */
    void celebrateClassLevelUp(Player player, String classDisplayName, int newLevel);

    /** Sends the invoking player a chat-only, non-broadcast skill level-up message. */
    void celebrateSkillLevelUp(Player player, String skillDisplayName, int newLevel);

    /** Bossbar+title/subtitle fade for the invoking player only, fired when derived Güç crosses a multiple of 10. */
    void celebrateGucMilestone(Player player, int newGuc);

    /**
     * The arrival announcement for {@code /rtp}: an on-screen title carrying the
     * coordinates the player landed on, so the first thing they see somewhere
     * unfamiliar is where they actually are.
     */
    void celebrateRandomTeleport(Player player, String x, String y, String z);

    /** Plays the "a world boss has appeared" cue - the chat announcement carrying its name and coordinates is a broadcast. */
    void celebrateBossSpawned(Player player);

    /** Plays the "a world boss has fallen" cue for everyone online. */
    void celebrateBossDefeated(Player player);

    /**
     * The Zenginlik Puanı milestone cue for the player who crossed it. Only the
     * sound and personal chat line live here - the server-wide announcement is
     * a broadcast the caller sends, because it is addressed to everyone else.
     */
    void celebrateWealthMilestone(Player player, long newTotal);

    /** Plays the class-selection sound cue only - the confirmation chat message is sent separately by the caller. */
    void celebrateClassSelected(Player player);

    /**
     * The full "Sınıf Değiştirildi" announcement for the changing player alone:
     * on-screen title, bossbar, personal chat line and sound cue together.
     * Unlike {@link #celebrateClassSelected} this one owns its own chat message,
     * because the three surfaces are meant to carry the same wording.
     */
    void celebrateClassChanged(Player player, String classDisplayName);

    /** Plays the short confirmation cue for picking an ability in the menu - the chat line is sent by the caller. */
    void celebrateAbilitySelected(Player player);

    /** Plays the ability-activation-start sound cue only. */
    void celebrateAbilityActivated(Player player);

    /** Plays the ability-activation-end sound cue only. */
    void celebrateAbilityEnded(Player player);
}
