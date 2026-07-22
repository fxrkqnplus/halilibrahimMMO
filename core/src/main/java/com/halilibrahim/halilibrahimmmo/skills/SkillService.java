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
