package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.locale.MessageKey;
import com.halilibrahim.halilibrahimmmo.locale.MessageService;
import com.halilibrahim.halilibrahimmmo.locale.NumberFormatter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The server-wide half of Saygınlık: every so often, one line about whoever is
 * moving fastest - in either direction.
 *
 * <p><strong>A running average per player, which no player is ever shown.</strong>
 * The number itself is not interesting - "your reputation is trending down" is
 * a dashboard, not a game. What is interesting is the one player on the server
 * who is climbing hardest, and the one who is falling apart, and saying so out
 * loud is what turns a private score into a rivalry.</p>
 *
 * <p><strong>Why a timer and not a hook on the award path.</strong> A trend only
 * exists relative to time; there is no single award that "causes" one, and
 * recomputing every player's momentum on every block break would make this the
 * highest-frequency thing in the plugin. Sampling each online player's total
 * once a round and comparing it against the previous round answers the same
 * question for the cost of a map lookup.</p>
 *
 * <p><strong>One line a round, never a digest.</strong> The most extreme mover
 * is picked and everybody else is dropped. A round that named six people would
 * be a wall of text nobody reads, and the point is that being named means
 * something.</p>
 *
 * <p>The interval is deliberately re-rolled inside its configured band on every
 * round rather than being fixed, so the announcement never becomes a clock the
 * server can set its watch by.</p>
 */
public final class ReputationTrendAnnouncer {

    /** Below this, a round's movement is ordinary play and not worth a line. */
    private static final long NOTABLE_MOVEMENT = 2_000L;
    /** How many differently-worded ways there are to say each half of it. */
    private static final List<MessageKey> RISE_LINES = List.of(
            MessageKey.REPUTATION_TREND_RISE_1, MessageKey.REPUTATION_TREND_RISE_2,
            MessageKey.REPUTATION_TREND_RISE_3, MessageKey.REPUTATION_TREND_RISE_4,
            MessageKey.REPUTATION_TREND_RISE_5, MessageKey.REPUTATION_TREND_RISE_6
    );
    private static final List<MessageKey> FALL_LINES = List.of(
            MessageKey.REPUTATION_TREND_FALL_1, MessageKey.REPUTATION_TREND_FALL_2,
            MessageKey.REPUTATION_TREND_FALL_3, MessageKey.REPUTATION_TREND_FALL_4,
            MessageKey.REPUTATION_TREND_FALL_5, MessageKey.REPUTATION_TREND_FALL_6,
            MessageKey.REPUTATION_TREND_FALL_7
    );

    private final JavaPlugin plugin;
    private final ReputationService reputation;
    private final MessageService messages;
    /** Each online player's total at the end of the previous round. */
    private final ConcurrentHashMap<UUID, Long> lastSeenTotal = new ConcurrentHashMap<>();
    /** What last hit each player, so a death can be blamed on the right thing. */
    private final ConcurrentHashMap<UUID, ReputationDeathCause> lastAttacker = new ConcurrentHashMap<>();
    /** Points lost to deaths since the round began, which the fall lines quote. */
    private final ConcurrentHashMap<UUID, AtomicLong> lostThisRound = new ConcurrentHashMap<>();
    private ScheduledTask task;

    public ReputationTrendAnnouncer(JavaPlugin plugin, ReputationService reputation, MessageService messages) {
        this.plugin = plugin;
        this.reputation = reputation;
        this.messages = messages;
    }

    /** Starts the loop, with the first round a full interval away rather than immediate. */
    public void start() {
        scheduleNextRound();
    }

    public void close() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastSeenTotal.clear();
        lastAttacker.clear();
        lostThisRound.clear();
    }

    /** Called by the death listener when it has actually taken points off somebody. */
    void recordLoss(UUID playerId, long amount) {
        lostThisRound.computeIfAbsent(playerId, ignored -> new AtomicLong()).addAndGet(amount);
    }

    /** Called by the death listener on every hit, so the death itself can be attributed. */
    void rememberAttacker(UUID playerId, ReputationDeathCause cause) {
        lastAttacker.put(playerId, cause);
    }

    /**
     * What last hurt this player, consumed as it is read.
     *
     * <p>Consuming it is what keeps a death honest: a player mauled by a zombie
     * at dawn and then drowned at noon must be recorded as having drowned, not
     * as still being the zombie's business.</p>
     */
    ReputationDeathCause recentCause(UUID playerId) {
        ReputationDeathCause remembered = lastAttacker.remove(playerId);
        return remembered == null ? ReputationDeathCause.KENDI : remembered;
    }

    private void scheduleNextRound() {
        long delayTicks = reputation.settings().trendIntervalTicks();
        task = plugin.getServer().getGlobalRegionScheduler()
                .runDelayed(plugin, ignored -> {
                    runRound();
                    scheduleNextRound();
                }, delayTicks);
    }

    /**
     * One sample of every online player, one comparison against the previous
     * sample, and at most one line.
     */
    private void runRound() {
        Player loudest = null;
        long extreme = 0L;
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            online.add(playerId);
            long total = reputation.cached(playerId);
            Long previous = lastSeenTotal.put(playerId, total);
            if (previous == null) {
                // First sighting: this round is their baseline, not their trend.
                continue;
            }
            long movement = total - previous;
            if (Math.abs(movement) > Math.abs(extreme)) {
                extreme = movement;
                loudest = player;
            }
        }
        long loss = loudest == null ? 0L : lostThisRound
                .getOrDefault(loudest.getUniqueId(), new AtomicLong(Math.abs(extreme))).get();
        // Every per-round tally resets whether or not its owner was named - it
        // describes this round, not a history - and players who have logged out
        // are dropped in the same pass, so neither map outlives the session.
        lostThisRound.clear();
        lastSeenTotal.keySet().retainAll(online);
        lastAttacker.keySet().retainAll(online);
        if (loudest == null || Math.abs(extreme) < NOTABLE_MOVEMENT) {
            return;
        }
        announce(loudest, extreme, loss);
    }

    private void announce(Player player, long movement, long lostThisRoundTotal) {
        boolean rising = movement > 0;
        List<MessageKey> pool = rising ? RISE_LINES : FALL_LINES;
        MessageKey chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        messages.broadcast(chosen, Map.of(
                "oyuncu", player.getName(),
                // The fall lines quote what was actually taken by deaths, which
                // is the number the player themselves saw; the rise lines quote
                // the round's net gain. Both placeholders are always supplied so
                // an owner can reword either line into the other shape.
                "degisim", NumberFormatter.compact(Math.abs(movement)),
                "dusus", NumberFormatter.compact(rising ? Math.abs(movement) : lostThisRoundTotal),
                "sayginlik", NumberFormatter.compact(reputation.cached(player.getUniqueId()))
        ));
    }
}
