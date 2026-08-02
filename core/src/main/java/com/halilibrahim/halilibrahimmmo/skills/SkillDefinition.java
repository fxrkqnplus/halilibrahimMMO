package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;

import java.util.Map;
import java.util.Set;

/**
 * Immutable YAML-authored skill definition. XP is rarity-tiered per source:
 * {@code materialRewards} for a {@link SkillSource#BLOCK} skill (only blocks
 * present in the map are eligible at all - same gate as before, just with a
 * per-material XP value instead of one flat reward), or {@code entityRewards}
 * plus {@code defaultEntityReward} for the {@link SkillSource#MOB} skill (any
 * non-player kill counts, falling back to the default entry for an
 * {@link EntityType} not explicitly listed).
 *
 * <p>{@code actionRewards} is orthogonal to {@code source}: any skill may also
 * declare per-{@link SkillAction} rewards for discrete gameplay moments that
 * are neither a block break nor a mob kill (breeding, shearing, trading,
 * tilling, ...). A {@link SkillSource#ACTION} skill has <em>only</em> those.</p>
 *
 * <p>{@code icon} is the item every screen represents this skill with - the
 * skills list, the per-skill detail screen its own command opens, and its
 * leaderboard - so a skill reads as itself at a glance instead of as one more
 * identical experience bottle.</p>
 *
 * <p>{@code milestones} is this skill's differentiated 25/50/75/100 passive
 * boost catalogue (Phase 12), keyed by the exact tier level - each skill is
 * free to grant a different {@link SkillMilestoneEffectType} at each tier,
 * replacing the earlier one-size-fits-all {@code MAX_HEALTH}-per-tier
 * formula every skill used to share.</p>
 */
public record SkillDefinition(
        String id,
        String displayName,
        Material icon,
        NamespacedKey model,
        SkillSource source,
        Map<Material, SkillRewardEntry> materialRewards,
        Map<EntityType, SkillRewardEntry> entityRewards,
        SkillRewardEntry defaultEntityReward,
        Map<SkillAction, SkillActionReward> actionRewards,
        Map<Integer, SkillMilestoneEffect> milestones
) {
    /** Convenience view over {@link #materialRewards}'s keys - the set of eligible block materials. */
    public Set<Material> materials() {
        return materialRewards.keySet();
    }
}
