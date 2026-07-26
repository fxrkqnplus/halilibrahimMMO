package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.ProgressionService;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

/**
 * Everything that has to happen <em>after</em> a skill XP award lands: the XP
 * bossbar, the skill level-up celebration, recomputed milestone effects and
 * the Güç milestone bossbar/broadcast.
 *
 * <p>Extracted in Phase 15 rather than copied a third time. Until then only
 * {@code SkillListener} awarded skill XP, so this lived as a private method
 * there; the husbandry and farming listeners award through exactly the same
 * path and must produce exactly the same feedback, and three copies of it is
 * past the point where this codebase's usual tolerance for small duplication
 * pays for itself.</p>
 */
public final class SkillAwardHandler {

    private static final int[] GUC_BROADCAST_MILESTONES = {25, 50, 75, 100};
    private static final int GUC_BOSSBAR_STEP = 10;

    private final JavaPlugin plugin;
    private final SkillService skills;
    private final FeedbackService feedback;
    private final ProgressionService progression;
    private final MessageService messages;

    public SkillAwardHandler(
            JavaPlugin plugin, SkillService skills, FeedbackService feedback,
            ProgressionService progression, MessageService messages
    ) {
        this.plugin = plugin;
        this.skills = skills;
        this.feedback = feedback;
        this.progression = progression;
        this.messages = messages;
    }

    /** Creative/Spectator players never earn skill, class or Güç experience - no risk, no reward. */
    public static boolean isGameplayGameMode(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    public void handle(Player player, Optional<SkillAwardResult> result) {
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
        if (newGuc / GUC_BOSSBAR_STEP > previousGuc / GUC_BOSSBAR_STEP) {
            feedback.celebrateGucMilestone(player, newGuc);
        }
        for (int milestone : GUC_BROADCAST_MILESTONES) {
            if (previousGuc < milestone && newGuc >= milestone) {
                messages.broadcast(MessageKey.GUC_BROADCAST, Map.of("oyuncu", player.getName(), "guc", Integer.toString(newGuc)));
            }
        }
    }
}
