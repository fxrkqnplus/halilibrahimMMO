package com.halilibrahim.halilibrahimmmo.reputation;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;
import java.util.Map;

/**
 * The immutable {@code sayginlik.yml} snapshot.
 *
 * <p>{@code milestones} is kept sorted ascending; every value in it is a total
 * a player can cross exactly once, which is what makes crossing one worth
 * announcing to the whole server.</p>
 *
 * <p>{@code rankAnnouncementIntervalMillis} is how often the leaderboard is
 * re-read for the rank-change announcements. It is a long interval by design:
 * every one of those announcements is a broadcast to the entire server, and a
 * short one would turn the standings into a running commentary nobody can mute.</p>
 *
 * @param trendMinimumMinutes the shortest gap between two trend announcements
 * @param trendMaximumMinutes the longest. The gap is re-rolled inside this band
 *                            on every round rather than being fixed, so the
 *                            announcement never becomes a clock the server can
 *                            set its watch by - which is what turns a recurring
 *                            broadcast back into news.
 */
public record ReputationSettings(
        Map<Material, Long> blockValues,
        Map<EntityType, Long> entityValues,
        long defaultEntityValue,
        Map<String, Long> questValues,
        long defaultQuestValue,
        List<Long> milestones,
        long rankAnnouncementIntervalMillis,
        long trendMinimumMinutes,
        long trendMaximumMinutes
) {

    private static final long TICKS_PER_MINUTE = 20L * 60L;

    /** A fresh delay for the next trend round, drawn from the configured band. */
    public long trendIntervalTicks() {
        long minutes = trendMinimumMinutes >= trendMaximumMinutes
                ? trendMinimumMinutes
                : java.util.concurrent.ThreadLocalRandom.current()
                        .nextLong(trendMinimumMinutes, trendMaximumMinutes + 1);
        return minutes * TICKS_PER_MINUTE;
    }

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
