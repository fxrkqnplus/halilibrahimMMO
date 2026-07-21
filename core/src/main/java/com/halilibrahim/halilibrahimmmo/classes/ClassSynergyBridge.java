package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.skills.SkillExperienceListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges skill XP gains into the class-level track: whenever a player gains
 * XP in their currently selected class's one synergy skill, this awards the
 * same amount as class-track XP and, on a class level-up, celebrates it on
 * the player's own region scheduler (a no-op if they have since gone offline).
 *
 * <p>Takes a forward-reference {@link AtomicReference} rather than a concrete
 * {@link ClassService} because {@code SkillService} (which invokes this) is
 * constructed before {@code ClassService} in the plugin bootstrap - the same
 * pattern already used to bridge {@code AbilityService} to {@code ClassService}.</p>
 */
public final class ClassSynergyBridge implements SkillExperienceListener {

    private final JavaPlugin plugin;
    private final ProfileService profiles;
    private final AtomicReference<ClassService> classServiceReference;
    private final FeedbackService feedback;

    public ClassSynergyBridge(
            JavaPlugin plugin, ProfileService profiles,
            AtomicReference<ClassService> classServiceReference, FeedbackService feedback
    ) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.classServiceReference = classServiceReference;
        this.feedback = feedback;
    }

    @Override
    public void onSkillExperienceGained(UUID playerId, String playerName, String skillId, long grantedAmount) {
        ClassService classes = classServiceReference.get();
        if (classes == null) {
            return;
        }
        profiles.cached(playerId).ifPresent(profile -> {
            String classId = profile.selectedClass();
            if (classId.isEmpty()) {
                return;
            }
            PlayerClassDefinition definition = classes.settings().definitions().get(classId);
            if (definition == null || !definition.synergySkillId().equals(skillId)) {
                return;
            }
            classes.awardClassExperience(playerId, playerName, classId, grantedAmount).thenAccept(result -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) {
                    return;
                }
                ClassLevelProgress progress = classes.classProgress(playerId, classId);
                long requiredForLevel = progress.level() >= classes.settings().classLevelCurve().maximumLevel()
                        ? 0L
                        : classes.settings().classLevelCurve().requiredForCurrentLevel(progress.level());
                player.getScheduler().run(plugin, task -> {
                    feedback.showExperienceProgress(
                            player, MessageKey.FEEDBACK_XP_CLASS_BOSSBAR,
                            Map.of("sinif", definition.displayName(), "kazanilan", Long.toString(grantedAmount)),
                            progress.experience(), requiredForLevel, "class", definition.barColor().color()
                    );
                    if (result.leveledUp()) {
                        feedback.celebrateClassLevelUp(player, definition.displayName(), result.newLevel());
                    }
                }, null);
            });
        });
    }
}
