package com.halilibrahim.halilibrahimmmo.onboarding;

import com.halilibrahim.halilibrahimmmo.classes.ClassAwardHandler;
import com.halilibrahim.halilibrahimmmo.classes.ClassService;
import com.halilibrahim.halilibrahimmmo.feedback.FeedbackService;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.scoreboard.SidebarService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Completes one onboarding nudge and pays out its reward.
 *
 * <p>The four call sites (opening the main menu, the skills screen, the
 * abilities screen, and any per-skill command) each carried their own copy of
 * this, and every copy fired the class XP award and then dropped the result on
 * the floor - so a tutorial task that happened to push a player over a class
 * level boundary levelled them up in complete silence. Routing through
 * {@link ClassAwardHandler} is what gives those level-ups the same chat line,
 * sound and bossbar every other XP source produces.</p>
 */
public final class OnboardingRewarder {

    private final JavaPlugin plugin;
    private final OnboardingService onboarding;
    private final ProfileService profiles;
    private final ClassService classes;
    private final MessageService messages;
    private final SidebarService sidebar;
    private final ClassAwardHandler classAwards;
    private final FeedbackService feedback;

    public OnboardingRewarder(
            JavaPlugin plugin, OnboardingService onboarding, ProfileService profiles, ClassService classes,
            MessageService messages, SidebarService sidebar, ClassAwardHandler classAwards, FeedbackService feedback
    ) {
        this.plugin = plugin;
        this.onboarding = onboarding;
        this.profiles = profiles;
        this.classes = classes;
        this.messages = messages;
        this.sidebar = sidebar;
        this.classAwards = classAwards;
        this.feedback = feedback;
    }

    /** Idempotent: a task already completed pays nothing and says nothing. */
    public void complete(Player player, OnboardingTask task, MessageKey completedMessage) {
        onboarding.complete(player.getUniqueId(), player.getName(), task).thenAccept(newlyCompleted -> {
            if (!newlyCompleted) {
                return;
            }
            String classId = profiles.cached(player.getUniqueId()).map(profile -> profile.selectedClass()).orElse("");
            if (!classId.isEmpty()) {
                classes.awardClassExperience(player.getUniqueId(), player.getName(), classId, task.rewardExperience())
                        .thenAccept(result -> classAwards.handle(player, classId, task.rewardExperience(), result));
            }
            // Read after the write, so "was this the last one" is answered by
            // the same cache the sidebar is about to render from - not by
            // counting the four ids here and risking the two disagreeing.
            boolean finishedAll = OnboardingTask.allCompleted(onboarding.cachedCompleted(player.getUniqueId()));
            player.getScheduler().run(plugin, scheduled -> {
                messages.send(player, completedMessage, Map.of("xp", Long.toString(task.rewardExperience())));
                if (finishedAll) {
                    feedback.celebrateOnboardingCompleted(player);
                }
                sidebar.refresh(player);
            }, null);
        });
    }
}
