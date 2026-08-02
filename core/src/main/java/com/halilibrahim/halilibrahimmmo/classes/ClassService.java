package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.api.classes.PlayerClassSnapshot;
import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.profile.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Owns configured classes, shared talent budgets, selections and active modifiers. */
public interface ClassService extends PluginService {
    ClassSettings settings();
    Collection<PlayerClassDefinition> definitions();
    String displayName(String classId);
    CompletableFuture<PlayerClassProgress> load(UUID playerId, String playerName);
    CompletableFuture<ClassSelectionResult> select(UUID playerId, String playerName, String classId);
    CompletableFuture<TalentSpendResult> spend(UUID playerId, String playerName, String talentId);
    CompletableFuture<Optional<PlayerClassSnapshot>> findSnapshot(UUID playerId);
    ClassModifiers modifiers(UUID playerId);
    void applyPlayerEffects(Player player);
    void evict(UUID playerId);

    /** How many points of one pool this player has earned in total (Güç cadence plus prestige grants). */
    int pointsFor(PlayerProfile profile, TalentPointCategory category);

    /**
     * Earned minus already-spent points of one pool - what the player may still
     * spend right now.
     *
     * <p>"Spent" covers more than talent ranks: the cloud chest's widenings are
     * bought out of the feature pool too, and are counted here rather than at
     * each screen that shows the number, so no caller can forget them.</p>
     */
    int availablePoints(PlayerProfile profile, PlayerClassProgress progress, TalentPointCategory category);

    /**
     * Internal, admin-only: moves this player's granted points in one pool by
     * {@code delta}, floored at zero, and answers with the pool's new grant.
     *
     * <p>The grant is stored separately from the derived budget rather than
     * written into it, because the budget recomputes from Güç and prestige on
     * every read - anything written there would be gone the next time the
     * player levelled. Works for an offline target, like every other admin
     * mutation here.</p>
     */
    CompletableFuture<Integer> adminAdjustTalentPoints(UUID playerId, String playerName, TalentPointCategory category, int delta);

    /** How many points of one pool an admin has granted this player. In-memory, warmed on load. */
    int grantedPoints(UUID playerId, TalentPointCategory category);

    /**
     * Internal: awards class-track XP to one specific (player, class) pair
     * through the shared per-player update chain, independent of whether
     * that class is currently selected. Callers (the skill-synergy bridge)
     * are responsible for only calling this for the player's selected class.
     */
    CompletableFuture<ClassExperienceAwardResult> awardClassExperience(UUID playerId, String playerName, String classId, long amount);

    /**
     * Internal: same as {@link #awardClassExperience}, but does not apply the
     * prestige multiplier - for callers whose {@code amount} already carries
     * it (currently only the skill-synergy bridge, since a synergy skill's
     * own XP gain is already prestige-adjusted before being mirrored here).
     */
    CompletableFuture<ClassExperienceAwardResult> awardClassExperienceExact(UUID playerId, String playerName, String classId, long amount);

    /** Synchronous, in-memory cached read of one (player, class) level+XP - level 1/XP 0 default if never loaded. */
    ClassLevelProgress classProgress(UUID playerId, String classId);

    /**
     * Internal: resets one specific (player, class) level/XP pair back to 1/0,
     * through the shared per-player update chain. Used by prestige to reset
     * only the player's currently-selected class - other classes' levels are
     * untouched.
     */
    CompletableFuture<Void> resetClassLevel(UUID playerId, String playerName, String classId);

    /** Internal, admin-only: force-sets a (player, class) level, clamped into the configured curve, resetting its XP to 0. Works for offline players too. */
    CompletableFuture<ClassExperienceAwardResult> adminSetClassLevel(UUID playerId, String playerName, String classId, int level);

    /** Internal, admin-only: adds an exact class-XP amount, no prestige multiplier. Works for offline players too. */
    CompletableFuture<ClassExperienceAwardResult> adminAddClassExperience(UUID playerId, String playerName, String classId, long amount);

    /**
     * Internal, admin-only: force-sets the player's selected class, bypassing
     * the change cooldown entirely and preserving spent talent ranks - an empty
     * string clears the class. Works for offline players too.
     */
    CompletableFuture<Void> adminForceSetClass(UUID playerId, String playerName, String classId);
}
