package com.halilibrahim.halilibrahimmmo.wealth;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardEntry;
import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import com.halilibrahim.halilibrahimmmo.leaderboards.LeaderboardService;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Announces movement on the Zenginlik leaderboard: who overtook whom, and who
 * is climbing through the standings without stopping.
 *
 * <p><strong>Why this is a timer and not a hook on the award path.</strong> A
 * rank only changes relative to everybody else, so there is no single award
 * that "causes" one - and hooking every award would mean re-ranking the whole
 * player base on every ore broken, which is the highest-frequency event in the
 * plugin. Reading the board on a long interval and diffing it against the last
 * reading answers the same question for the cost of one query.</p>
 *
 * <p><strong>One announcement per sweep, not one per change.</strong> Over
 * twenty-five minutes several people move, and broadcasting all of them turns
 * the standings into a wall of text. The single most notable change is chosen:
 * the biggest climb, and among equal climbs the one that ended highest.</p>
 *
 * <p>Each shape ships several interchangeable wordings, picked at random, for
 * the same reason the boss spawn cue does - a fixed sentence repeated on a
 * timer stops registering as news. Because the board is a top-N list, movement
 * entirely below that cut is invisible here; that is deliberate, since a change
 * nobody can see on the leaderboard is not one worth announcing either.</p>
 */
public final class WealthRankAnnouncer {

    private static final MessageKey[] TAKEOVER_TEMPLATES = {
            MessageKey.WEALTH_RANK_TAKEOVER_1, MessageKey.WEALTH_RANK_TAKEOVER_2,
            MessageKey.WEALTH_RANK_TAKEOVER_3, MessageKey.WEALTH_RANK_TAKEOVER_4,
            MessageKey.WEALTH_RANK_TAKEOVER_5, MessageKey.WEALTH_RANK_TAKEOVER_6,
            MessageKey.WEALTH_RANK_TAKEOVER_7, MessageKey.WEALTH_RANK_TAKEOVER_8
    };
    private static final MessageKey[] CLIMB_TEMPLATES = {
            MessageKey.WEALTH_RANK_CLIMB_1, MessageKey.WEALTH_RANK_CLIMB_2, MessageKey.WEALTH_RANK_CLIMB_3,
            MessageKey.WEALTH_RANK_CLIMB_4, MessageKey.WEALTH_RANK_CLIMB_5, MessageKey.WEALTH_RANK_CLIMB_6,
            MessageKey.WEALTH_RANK_CLIMB_7
    };
    private static final long TICKS_PER_MILLISECOND_DIVISOR = 50L;

    private final JavaPlugin plugin;
    private final MessageService messages;
    private final LeaderboardService leaderboards;
    private final WealthService wealth;
    /** The previous sweep's standings, by player name - the board carries no id. */
    private final Map<String, Integer> previousRanks = new HashMap<>();
    private ScheduledTask task;

    public WealthRankAnnouncer(
            JavaPlugin plugin, MessageService messages, LeaderboardService leaderboards, WealthService wealth
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.leaderboards = leaderboards;
        this.wealth = wealth;
    }

    /**
     * The first sweep is a full interval away rather than immediate, so the
     * standings it records are the baseline everything afterwards is compared
     * against - announcing every rank on the board at server start would greet
     * everyone with news that is not new.
     */
    public void start() {
        long periodTicks = Math.max(20L, wealth.settings().rankAnnouncementIntervalMillis() / TICKS_PER_MILLISECOND_DIVISOR);
        task = plugin.getServer().getGlobalRegionScheduler()
                .runAtFixedRate(plugin, ignored -> sweep(), periodTicks, periodTicks);
    }

    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        previousRanks.clear();
    }

    private void sweep() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            // Nobody to tell, and recording standings nobody moved through would
            // only make the next sweep announce stale news.
            return;
        }
        leaderboards.top(LeaderboardCategory.WEALTH).thenAccept(entries ->
                plugin.getServer().getGlobalRegionScheduler().run(plugin, ignored -> announce(entries)));
    }

    private void announce(List<LeaderboardEntry> entries) {
        Map<String, Integer> current = new HashMap<>();
        entries.forEach(entry -> current.put(entry.playerName(), entry.rank()));

        LeaderboardEntry best = null;
        int bestClimb = 0;
        for (LeaderboardEntry entry : entries) {
            Integer before = previousRanks.get(entry.playerName());
            if (before == null) {
                continue;
            }
            int climb = before - entry.rank();
            if (climb > bestClimb || (climb == bestClimb && best != null && entry.rank() < best.rank())) {
                bestClimb = climb;
                best = entry;
            }
        }
        if (best != null && bestClimb > 0) {
            broadcast(best, bestClimb, entries);
        }
        previousRanks.clear();
        previousRanks.putAll(current);
    }

    /**
     * A climber who took someone's exact place names both players; one who came
     * up through several places at once is worth reporting on their own, and
     * naming only the person they happened to land on would understate it.
     */
    private void broadcast(LeaderboardEntry climber, int climb, List<LeaderboardEntry> entries) {
        String displaced = climb == 1 ? nameAt(entries, climber.rank() + 1) : "";
        if (!displaced.isEmpty()) {
            messages.broadcast(pick(TAKEOVER_TEMPLATES), Map.of(
                    "oyuncu", climber.playerName(),
                    "rakip", displaced,
                    "sira", Integer.toString(climber.rank()),
                    "zenginlik", NumberFormatter.compact(climber.primaryValue())
            ));
            return;
        }
        messages.broadcast(pick(CLIMB_TEMPLATES), Map.of(
                "oyuncu", climber.playerName(),
                "sira", Integer.toString(climber.rank()),
                "basamak", Integer.toString(climb),
                "zenginlik", NumberFormatter.compact(climber.primaryValue())
        ));
    }

    /** Who currently sits at {@code rank}, or empty when that rank is past the end of the board. */
    private String nameAt(List<LeaderboardEntry> entries, int rank) {
        return entries.stream()
                .filter(entry -> entry.rank() == rank)
                .map(LeaderboardEntry::playerName)
                .findFirst()
                .orElse("");
    }

    private MessageKey pick(MessageKey[] templates) {
        return templates[ThreadLocalRandom.current().nextInt(templates.length)];
    }
}
