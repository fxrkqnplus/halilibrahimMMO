package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.abilities.AbilityDefinition;
import com.halilibrahim.halilibrahimmmo.abilities.AbilityService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.progression.FeatureUnlockRules;
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
    private final MessageService messages;

    public ClassAwardHandler(
            JavaPlugin plugin,
            AtomicReference<ClassService> classServiceReference,
            AtomicReference<AbilityService> abilityServiceReference,
            FeedbackService feedback,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.classServiceReference = classServiceReference;
        this.abilityServiceReference = abilityServiceReference;
        this.feedback = feedback;
        this.messages = messages;
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
        // Same reasoning as {@code SkillAwardHandler}: a class at its cap keeps
        // being handed XP by every synergy skill, and drawing a "+120 XP" bar
        // against a track that cannot move is feedback for something that did
        // not happen.
        boolean maxedOut = progress.level() >= classes.settings().classLevelCurve().maximumLevel();
        player.getScheduler().run(plugin, task -> {
            if (!maxedOut) {
                feedback.showExperienceProgress(
                        player, MessageKey.FEEDBACK_XP_CLASS_BOSSBAR,
                        Map.of(
                                "sinif", definition.displayName(),
                                "seviye", Integer.toString(progress.level()),
                                "kazanilan", Long.toString(grantedAmount)
                        ),
                        progress.experience(),
                        classes.settings().classLevelCurve().requiredForCurrentLevel(progress.level()),
                        "class", definition.barColor().color()
                );
            }
            if (result.leveledUp()) {
                feedback.celebrateClassLevelUp(player, definition.displayName(), result.newLevel());
                notifyUnlockedAbilities(player, classId, result.previousLevel(), result.newLevel());
                notifyUnlockedFeatures(player, result.previousLevel(), result.newLevel());
            }
        }, null);
    }

    /**
     * {@code /ev} and {@code /takas} open at fixed class levels and nothing was
     * telling a player when: the sidebar's feature line simply changed from
     * "Kilitli" to "Açık" one level, which nobody is looking at in the middle of
     * a fight. Announced here rather than from the home/trade services because
     * this is the moment the level actually changed - those services only ever
     * answer "is it open now".
     *
     * <p>Only the class-level path announces. Prestige also satisfies the gate
     * (see {@code FeatureUnlockRules}), but a prestiging player has had both
     * features for a long time and does not need to be told they still have
     * them.</p>
     */
    private void notifyUnlockedFeatures(Player player, int previousLevel, int newLevel) {
        if (crossed(FeatureUnlockRules.HOME_CLASS_LEVEL, previousLevel, newLevel)) {
            messages.send(player, MessageKey.FEATURE_UNLOCKED_HOME);
        }
        if (crossed(FeatureUnlockRules.TRADE_CLASS_LEVEL, previousLevel, newLevel)) {
            messages.send(player, MessageKey.FEATURE_UNLOCKED_TRADE);
        }
    }

    /** Handles multi-level jumps, exactly like the ability-unlock check above it. */
    private static boolean crossed(int threshold, int previousLevel, int newLevel) {
        return threshold > previousLevel && threshold <= newLevel;
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
