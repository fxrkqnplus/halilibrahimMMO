package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Everything that has to happen <em>after</em> class XP lands: the class XP
 * bossbar in the class's own colour, the level-up chat line with its sound cue,
 * and the "new ability unlocked" toast.
 *
 * <p>Extracted for the same reason {@code SkillAwardHandler} was. Class XP is
 * awarded from six independent places - synergy-skill mirroring, quest
 * completion, collection entries, collection completion, and the four
 * onboarding nudges - and each had grown its own partial copy of this block, or
 * in the onboarding case no copy at all, so finishing a tutorial task could
 * silently level a class with no chat line and no sound at all. One handler
 * makes every source of class XP indistinguishable from every other, which is
 * exactly what a player expects from a level-up.</p>
 *
 * <p>Both collaborators arrive as {@link AtomicReference} forward references
 * because the plugin bootstrap constructs this handler's first consumer
 * ({@code SkillService}, via {@code ClassSynergyBridge}) before either
 * {@code ClassService} or {@code AbilityService} exists - the same bridge
 * pattern those two services already use on each other.</p>
 */
public final class ClassAwardHandler {

    private final JavaPlugin plugin;
    private final AtomicReference<ClassService> classServiceReference;
    private final AtomicReference<AbilityService> abilityServiceReference;
    private final FeedbackService feedback;

    public ClassAwardHandler(
            JavaPlugin plugin,
            AtomicReference<ClassService> classServiceReference,
            AtomicReference<AbilityService> abilityServiceReference,
            FeedbackService feedback
    ) {
        this.plugin = plugin;
        this.classServiceReference = classServiceReference;
        this.abilityServiceReference = abilityServiceReference;
        this.feedback = feedback;
    }

    /**
     * Renders the full feedback set for one class XP award. Safe to call from
     * any thread: everything touching Bukkit is hopped onto the player's own
     * region scheduler first, and a player who has since gone offline simply
     * gets nothing.
     *
     * @param grantedAmount the amount actually credited, after every multiplier
     */
    public void handle(Player player, String classId, long grantedAmount, ClassExperienceAwardResult result) {
        ClassService classes = classServiceReference.get();
        if (classes == null || grantedAmount <= 0) {
            return;
        }
        PlayerClassDefinition definition = classes.settings().definitions().get(classId);
        if (definition == null) {
            return;
        }
        ClassLevelProgress progress = classes.classProgress(player.getUniqueId(), classId);
        long requiredForLevel = progress.level() >= classes.settings().classLevelCurve().maximumLevel()
                ? 0L
                : classes.settings().classLevelCurve().requiredForCurrentLevel(progress.level());
        player.getScheduler().run(plugin, task -> {
            feedback.showExperienceProgress(
                    player, MessageKey.FEEDBACK_XP_CLASS_BOSSBAR,
                    Map.of(
                            "sinif", definition.displayName(),
                            "seviye", Integer.toString(progress.level()),
                            "kazanilan", Long.toString(grantedAmount)
                    ),
                    progress.experience(), requiredForLevel, "class", definition.barColor().color()
            );
            if (result.leveledUp()) {
                feedback.celebrateClassLevelUp(player, definition.displayName(), result.newLevel());
                notifyUnlockedAbilities(player, classId, result.previousLevel(), result.newLevel());
            }
        }, null);
    }

    /** Handles multi-level jumps, not just +1 increments - an ability unlocked mid-jump still announces itself. */
    private void notifyUnlockedAbilities(Player player, String classId, int previousLevel, int newLevel) {
        AbilityService abilities = abilityServiceReference.get();
        if (abilities == null) {
            return;
        }
        for (AbilityDefinition ability : abilities.definitions()) {
            if (!ability.classId().equals(classId)) {
                continue;
            }
            if (ability.requiredLevel() > previousLevel && ability.requiredLevel() <= newLevel) {
                feedback.celebrateAbilityUnlocked(player, ability.displayName());
            }
        }
    }
}
