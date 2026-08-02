package com.halilibrahim.halilibrahimmmo.quests;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;

import java.util.Optional;
import java.util.Set;

/**
 * Immutable YAML-authored quest definition.
 *
 * <p>Exactly one of the target sets - or {@link #deed} - is meaningful, chosen
 * by {@link #objectiveType}: {@code materials} for
 * {@link QuestObjectiveType#BLOCK_BREAK}, {@code entityTypes} for
 * {@link QuestObjectiveType#MOB_KILL} (empty meaning "any non-player kill",
 * mirroring {@code skills.SkillSource#MOB}), {@code structures} for
 * {@link QuestObjectiveType#STRUCTURE_DISCOVER}, {@code items} for
 * {@link QuestObjectiveType#ITEM_OBTAIN} and {@code deed} for
 * {@link QuestObjectiveType#DEED}. The unused ones are always empty - the same
 * "every field present, only the relevant one populated" convention this record
 * has followed since it had two.</p>
 *
 * <p>{@code skillId} names the skill this quest trains. Completing it credits
 * that skill's own XP through the ordinary skill award path, which in turn
 * mirrors into the class track whenever the skill is one the player's selected
 * class draws synergy from - so a hunting quest advances avcılık for everyone
 * and Amansız Savaşçı as well for a player who is one.</p>
 *
 * @param shortName  what the sidebar shows. The full display name is written for
 *                   a menu tooltip and routinely runs past what a scoreboard
 *                   line can hold, where an overrun does not wrap - it is simply
 *                   cut off mid-word.
 * @param dimension  restricts a block or kill objective to one world type, so
 *                   "kill a hundred endermen in the End" can mean the End rather
 *                   than a plains biome at night. Empty means anywhere.
 */
public record QuestDefinition(
        String id,
        String displayName,
        String shortName,
        String description,
        Material icon,
        NamespacedKey model,
        QuestType type,
        String skillId,
        QuestObjectiveType objectiveType,
        Set<Material> materials,
        Set<EntityType> entityTypes,
        Set<Structure> structures,
        Set<Material> items,
        Optional<QuestDeed> deed,
        Optional<World.Environment> dimension,
        int target,
        long rewardExperience
) {

    /** Whether an event that happened in {@code environment} counts toward this quest. */
    public boolean acceptsDimension(World.Environment environment) {
        return dimension.isEmpty() || dimension.get() == environment;
    }
}
