package com.halilibrahim.halilibrahimmmo.boss;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * World bosses: one named, rarity-tiered creature spawned somewhere on the map
 * at a fixed interval, announced to everyone, and worth killing.
 *
 * <p>A boss is nothing but a heavily modified vanilla mob carrying a
 * {@code PersistentDataContainer} tag, which is what makes the whole system
 * survive a restart without any table of its own: the server saves the entity,
 * the tag rides along with it, and the reward path reads the tag rather than an
 * in-memory registry. The one thing a restart does cost is the flee/heal
 * behaviour loop, which is re-attached only for bosses spawned since - an old
 * boss keeps its stats, its name and its full reward, and simply fights like a
 * very strong ordinary mob.</p>
 */
public interface BossService extends PluginService {

    /**
     * How often each living boss re-evaluates what it is doing and repaints its
     * health bar.
     *
     * <p>Half a second is chosen by the bar rather than by the behaviour: a
     * health bar that lags a fight by two seconds reads as broken. Every
     * behaviour decision is independently gated on its own millisecond interval
     * and does not care how often it is asked.</p>
     */
    long BEHAVIOUR_PERIOD_TICKS = 10L;

    /**
     * {@link #BEHAVIOUR_PERIOD_TICKS} as a fraction of a second, so a
     * per-second rate in {@code bosslar.yml} can be scaled to one pass without
     * that conversion being written out - and re-derived - at each call site.
     */
    double BEHAVIOUR_PERIOD_SECONDS = BEHAVIOUR_PERIOD_TICKS / 20.0D;

    BossSettings settings();

    /** Whether this creature is a world boss - i.e. carries the boss tag. */
    boolean isBoss(LivingEntity entity);

    /** The rarity tag on this creature, or empty if it is not a boss. */
    Optional<BossRarity> rarityOf(LivingEntity entity);

    /**
     * Records that a player has actually fought this boss, which is what
     * protects it from being cleared away when the next one spawns.
     *
     * <p>An untouched boss is map clutter: nobody went to it, and leaving it
     * standing only means the next announcement competes with a stale one. A
     * boss somebody is <em>mid-fight</em> with is the opposite, and must never
     * vanish out from under them - so engagement, not age, is the test.</p>
     */
    void markEngaged(LivingEntity entity);

    /**
     * Takes this boss's health bar off every screen still showing it and forgets
     * its behaviour state. Called when it dies; a no-op for a boss this session
     * never spawned (one that outlived a restart), which has no bar to take
     * down in the first place.
     */
    void retire(UUID bossId);

    /**
     * Spawns one boss right now near a randomly chosen online player, ignoring
     * the interval timer. Used by the admin panel.
     *
     * <p>Asynchronous because the search genuinely is: a spot a few hundred
     * blocks from the anchor player is almost never in an already-loaded chunk,
     * so each candidate has to be loaded before it can be inspected. The future
     * completes with the boss's name only once it has actually spawned and been
     * announced, and empty when there was nobody to spawn near or nowhere to
     * stand - which is what lets the caller tell an admin the truth instead of
     * naming a boss that never arrived.</p>
     */
    CompletableFuture<Optional<String>> spawnNow();

    /**
     * The same spawn, with either half chosen by an admin instead of rolled.
     *
     * @param rarity the tier to spawn, or {@code null} to roll one by weight
     * @param name   the boss's name, or {@code null} to draw one from the
     *               archetype's own pool
     */
    CompletableFuture<Optional<String>> spawnNow(BossRarity rarity, String name);
}
