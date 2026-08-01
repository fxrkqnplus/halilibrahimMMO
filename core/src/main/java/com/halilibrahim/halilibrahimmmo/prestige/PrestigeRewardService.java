package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The permanent reward a player picks after each prestige, and what those
 * picks add up to.
 *
 * <p>Every rank grants exactly one choice, and an unspent choice is not
 * forgotten: it is derived as {@code prestige - (rewards already picked)}
 * rather than stored, so it survives a restart, a crash mid-menu and an admin
 * editing somebody's prestige count, with nothing to keep in step. The screen
 * that spends it is opened in front of the player until they do.</p>
 */
public interface PrestigeRewardService extends PluginService {

    /** How much one pick of each reward is worth - read from {@code prestige.yml}. */
    Map<PrestigeRewardType, Double> values();

    /** Warms this player's picks on join; every consumer below reads them synchronously. */
    CompletableFuture<Void> load(UUID playerId);

    /** Synchronous, in-memory only - empty until {@link #load} has warmed this player. */
    Map<PrestigeRewardType, Integer> cached(UUID playerId);

    /**
     * How many prestige ranks this player has not yet spent a choice on.
     *
     * @param prestige the player's current rank, passed in rather than resolved
     *                 here so this service never has to know about profiles
     */
    int pendingChoices(UUID playerId, int prestige);

    /**
     * Spends one pending choice on {@code type}.
     *
     * @return {@code false} when there was nothing to spend, so a second click
     *         arriving before the screen repaints cannot buy a second reward
     */
    CompletableFuture<Boolean> choose(UUID playerId, PrestigeRewardType type, int prestige);

    /**
     * How many prestige ranks this player has not yet taken a <em>crate</em>
     * for.
     *
     * <p>Counted separately from the permanent choice and derived the same way,
     * so the two cannot drift: a player who picked a permanent reward and then
     * logged out before taking their items still owes themselves the crate.</p>
     */
    int pendingPhysicalChoices(UUID playerId, int prestige);

    /**
     * Takes one crate.
     *
     * @return {@code false} when there was nothing to take, so a second click
     *         arriving before the screen repaints cannot claim two
     */
    CompletableFuture<Boolean> claimPhysical(UUID playerId, int prestige);

    /** Extra maximum health from this player's picks, in half-heart points. */
    double healthBonus(UUID playerId);

    /** Extra creature damage from this player's picks, as a fraction to add to the multiplier. */
    double damageBonus(UUID playerId);

    /** Extra maximum energy from this player's picks. */
    int energyBonus(UUID playerId);

    /** The skill-XP multiplier this player's picks are worth, {@code 1.0} when they have none. */
    double experienceMultiplier(UUID playerId);

    /**
     * Applies the health reward, which is the one of the four that has to be
     * written onto the player rather than read by somebody else. Must run on
     * the player's own region, and has to be called again after a respawn -
     * the server resets attributes after that event returns, the same way it
     * does for class and milestone health.
     */
    void applyPlayerEffects(Player player);

    void evict(UUID playerId);
}
