package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.profile.ProfileService;
import com.halilibrahim.halilibrahimmmo.skills.SkillExperienceListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges skill XP gains into the class-level track: whenever a player gains
 * XP in any of their currently selected class's synergy skills, this awards
 * the same amount as class-track XP and hands the result to
 * {@link ClassAwardHandler}, which owns every piece of player-facing feedback
 * a class XP award produces regardless of where it came from.
 *
 * <p>Takes a forward-reference {@link AtomicReference} rather than a concrete
 * {@link ClassService} because {@code SkillService} (which invokes this) is
 * constructed before it in the plugin bootstrap - the same pattern already
 * used to bridge {@code AbilityService} to {@code ClassService}.</p>
 */
public final class ClassSynergyBridge implements SkillExperienceListener {

    private final ProfileService profiles;
    private final AtomicReference<ClassService> classServiceReference;
    private final ClassAwardHandler awards;

    public ClassSynergyBridge(
            ProfileService profiles, AtomicReference<ClassService> classServiceReference, ClassAwardHandler awards
    ) {
        this.profiles = profiles;
        this.classServiceReference = classServiceReference;
        this.awards = awards;
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
            if (definition == null || !definition.hasSynergyWith(skillId)) {
                return;
            }
            classes.awardClassExperienceExact(playerId, playerName, classId, grantedAmount).thenAccept(result -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    awards.handle(player, classId, grantedAmount, result);
                }
            });
        });
    }
}
