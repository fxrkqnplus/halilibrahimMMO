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
 */
public record SkillDefinition(
        String id,
        String displayName,
        SkillSource source,
        Map<Material, SkillRewardEntry> materialRewards,
        Map<EntityType, SkillRewardEntry> entityRewards,
        SkillRewardEntry defaultEntityReward
) {
    /** Convenience view over {@link #materialRewards}'s keys - the set of eligible block materials. */
    public Set<Material> materials() {
        return materialRewards.keySet();
    }
}
