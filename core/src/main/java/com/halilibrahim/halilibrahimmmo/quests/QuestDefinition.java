package com.halilibrahim.halilibrahimmmo.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Set;

/**
 * Immutable YAML-authored quest definition. {@code entityTypes} is only
 * meaningful for {@link QuestObjectiveType#MOB_KILL} and an empty set there
 * means "any non-player kill", mirroring {@code skills.SkillSource#MOB}.
 */
public record QuestDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        QuestType type,
        QuestObjectiveType objectiveType,
        Set<Material> materials,
        Set<EntityType> entityTypes,
        int target,
        long rewardExperience
) {
}
