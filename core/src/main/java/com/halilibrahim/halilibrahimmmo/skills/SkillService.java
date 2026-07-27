package com.halilibrahim.halilibrahimmmo.skills;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous persistent skill progression contract. */
public interface SkillService extends PluginService {

    SkillSettings settings();

    Collection<SkillDefinition> definitions();

    CompletableFuture<Optional<SkillProgress>> find(UUID playerId, String skillId);

    CompletableFuture<SkillProgress> load(UUID playerId, String skillId);

    CompletableFuture<Void> loadAll(UUID playerId);

    CompletableFuture<Optional<SkillAwardResult>> awardBlock(UUID playerId, String playerName, Material material);

    CompletableFuture<Optional<SkillAwardResult>> awardMob(UUID playerId, String playerName, EntityType entityType);

    /**
     * Awards whichever skill declared {@code action} in {@code skills.yml}.
     * {@code context} is the item/block that caused the action for
     * material-gated actions (the cooked food, the crafted item) and
     * {@code null} for actions whose own event is the whole gate; a
     * non-matching context resolves to an empty result without awarding
     * anything. Empty is also returned when no configured skill claims the
     * action at all, so a server that removed a skill degrades quietly
     * instead of throwing on every event.
     */
    CompletableFuture<Optional<SkillAwardResult>> awardAction(UUID playerId, String playerName, SkillAction action, Material context);

    /** The configured reward for {@code action}, for listeners that need its material gate before doing any work. */
    Optional<SkillActionReward> actionReward(SkillAction action);

    /**
     * Awards an explicit amount to one named skill through the ordinary XP
     * path - same class-synergy and prestige multipliers, same serial update
     * chain, same notification to the class-synergy bridge - for rewards whose
     * size is authored outside {@code skills.yml}.
     *
     * <p>This is what a completed quest pays out with: the reward lands in the
     * skill the quest trains, and mirrors into the class track exactly when
     * that skill is one the player's selected class draws synergy from,
     * instead of being a bespoke class-XP grant that has to reimplement every
     * multiplier and every piece of feedback for itself.</p>
     *
     * @param sourceDisplayName already-Turkish label for the XP bossbar (e.g. the quest's own name)
     * @return empty when {@code skillId} names no configured skill
     */
    CompletableFuture<Optional<SkillAwardResult>> awardExternal(
            UUID playerId, String playerName, String skillId, long amount, String sourceDisplayName
    );

    /** Synchronous, in-memory only - for callers that cannot await a future (e.g. PlaceholderAPI). */
    Optional<SkillProgress> cached(UUID playerId, String skillId);

    /** Internal: resets every configured skill for this player back to level 1/XP 0 (used by prestige). */
    CompletableFuture<Void> resetAll(UUID playerId);

    /**
     * Internal, admin-only: force-sets a skill's level, clamped into
     * {@code [1, curve().maximumLevel()]}, resetting its XP to 0 - a
     * correction tool, not a gameplay XP path. Works for offline players too.
     */
    CompletableFuture<SkillAdminAwardResult> adminSetLevel(UUID playerId, String skillId, int level);

    /**
     * Internal, admin-only: adds an exact XP amount to a skill (no class-synergy
     * or prestige multiplier - admin grants are exact amounts). Works for
     * offline players too.
     */
    CompletableFuture<SkillAdminAwardResult> adminAddExperience(UUID playerId, String skillId, long amount);

    /**
     * Recomputes and reapplies this player's combined skill-milestone bonuses
     * (levels 25/50/75/100 per skill, cumulative, each skill differentiated -
     * see {@link SkillMilestoneEffectType}) from currently cached skill
     * levels - a stable {@code NamespacedKey}-modifier remove-then-reapply
     * for {@code MAX_HEALTH}, the same pattern {@code ClassService#applyPlayerEffects}
     * already uses, plus a potion-effect remove-then-reapply for
     * {@code HASTE}/{@code SPEED}.
     */
    void applyMilestoneEffects(Player player);

    /** Synchronous, in-memory only - the same milestone bonuses {@link #applyMilestoneEffects} applies, for consumers that need the raw numbers (energy cap, PvE damage). */
    SkillMilestoneModifiers milestoneModifiers(UUID playerId);

    void evict(UUID playerId);
}
