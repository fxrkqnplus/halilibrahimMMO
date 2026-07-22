package com.halilibrahim.halilibrahimmmo.skills;

import org.bukkit.Material;
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
 * <p>{@code milestones} is this skill's differentiated 25/50/75/100 passive
 * boost catalogue (Phase 12), keyed by the exact tier level - each skill is
 * free to grant a different {@link SkillMilestoneEffectType} at each tier,
 * replacing the earlier one-size-fits-all {@code MAX_HEALTH}-per-tier
 * formula every skill used to share.</p>
 */
public record SkillDefinition(
        String id,
        String displayName,
        SkillSource source,
        Map<Material, SkillRewardEntry> materialRewards,
        Map<EntityType, SkillRewardEntry> entityRewards,
        SkillRewardEntry defaultEntityReward,
        Map<Integer, SkillMilestoneEffect> milestones
) {
    /** Convenience view over {@link #materialRewards}'s keys - the set of eligible block materials. */
    public Set<Material> materials() {
        return materialRewards.keySet();
    }
}
