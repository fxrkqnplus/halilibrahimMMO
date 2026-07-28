package com.halilibrahim.halilibrahimmmo.wealth;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;

/**
 * The immutable {@code zenginlik.yml} snapshot.
 *
 * <p>{@code milestones} is kept sorted ascending; every value in it is a total
 * a player can cross exactly once, which is what makes crossing one worth
 * announcing to the whole server.</p>
 */
public record WealthSettings(
        Map<Material, Long> blockValues,
        Map<EntityType, Long> entityValues,
        long defaultEntityValue,
        Map<String, Long> questValues,
        long defaultQuestValue,
        List<Long> milestones
) {

    public long blockValue(Material material) {
        return blockValues.getOrDefault(material, 0L);
    }

    /**
     * Any creature is worth something - the map only decides how much. An
     * unlisted mob falls back to {@code defaultEntityValue}, which is
     * deliberately the floor, because the mobs nobody bothered to list are
     * almost always the farmable ones.
     */
    public long entityValue(EntityType type) {
        return entityValues.getOrDefault(type, defaultEntityValue);
    }

    public long questValue(String questId) {
        return questValues.getOrDefault(questId, defaultQuestValue);
    }

    /**
     * The highest milestone strictly between {@code previousTotal} and
     * {@code newTotal}, or {@code 0} if this award crossed none. Only the
     * highest is announced: a single boss kill can vault a new player past
     * several thresholds at once, and five broadcasts in one tick is noise.
     */
    public long crossedMilestone(long previousTotal, long newTotal) {
        long crossed = 0L;
        for (long milestone : milestones) {
            if (milestone > previousTotal && milestone <= newTotal) {
                crossed = milestone;
            }
        }
        return crossed;
    }
}
