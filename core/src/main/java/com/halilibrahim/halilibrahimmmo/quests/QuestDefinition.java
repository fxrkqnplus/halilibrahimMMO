package com.halilibrahim.halilibrahimmmo.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;

import java.util.Set;

/**
 * Immutable YAML-authored quest definition.
 *
 * <p>Exactly one of the four target sets is meaningful, chosen by
 * {@link #objectiveType}: {@code materials} for {@link QuestObjectiveType#BLOCK_BREAK},
 * {@code entityTypes} for {@link QuestObjectiveType#MOB_KILL} (empty meaning
 * "any non-player kill", mirroring {@code skills.SkillSource#MOB}),
 * {@code structures} for {@link QuestObjectiveType#STRUCTURE_DISCOVER} and
 * {@code items} for {@link QuestObjectiveType#ITEM_OBTAIN}. The unused ones are
 * always empty - the same "every field present, only the relevant one
 * populated" convention this record has followed since it had two.</p>
 *
 * <p>{@code skillId} names the skill this quest trains. Completing it credits
 * that skill's own XP through the ordinary skill award path, which in turn
 * mirrors into the class track whenever the skill is one the player's selected
 * class draws synergy from - so a hunting quest advances avcılık for everyone
 * and Amansız Savaşçı as well for a player who is one.</p>
 */
public record QuestDefinition(
        String id,
        String displayName,
        String description,
        Material icon,
        QuestType type,
        String skillId,
        QuestObjectiveType objectiveType,
        Set<Material> materials,
        Set<EntityType> entityTypes,
        Set<Structure> structures,
        Set<Material> items,
        int target,
        long rewardExperience
) {
}
