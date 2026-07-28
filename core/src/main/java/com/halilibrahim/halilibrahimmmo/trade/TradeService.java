package com.halilibrahim.halilibrahimmmo.trade;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Remote player-to-player item trading, unlocked at class level {@code 20} (or
 * for anyone who has ever prestiged). The other side must have reached class
 * level {@code 15}, or likewise have prestiged.
 *
 * <p>Deliberately <strong>in-memory only</strong>, with no repository and no
 * table: a trade is a conversation between two players who are both online
 * right now, and a half-finished one must not survive a restart - every
 * shutdown path returns the goods instead.</p>
 *
 * <p>No distance check anywhere. "Remote" is the whole point of the feature;
 * the two players may be in different worlds.</p>
 */
public interface TradeService extends PluginService {

    /** How long an unanswered trade request stands before it lapses. */
    long REQUEST_TIMEOUT_MILLIS = 60_000L;

    /** Synchronous, in-memory only: whether this player's class progress has unlocked {@code /takas}. */
    boolean isUnlocked(UUID playerId);

    /** The trade this player is currently in, if any. Synchronous - the sidebar reads it every render. */
    Optional<TradeSession> activeSession(UUID playerId);

    /** The name of whoever this player has an unanswered incoming request from, if any. */
    Optional<String> pendingRequestFrom(UUID playerId);

    TradeRequestResult request(Player sender, String targetName);

    /**
     * Accepts the pending request and opens the trade screen for both sides.
     *
     * @return {@code false} when nothing was pending (it lapsed, or was never sent)
     */
    boolean accept(Player player);

    /** @return {@code false} when nothing was pending */
    boolean decline(Player player);

    /** Moves one stack from the player's inventory onto their side of the table. */
    void offer(Player player, int inventorySlot);

    /** Takes one stack back off the table and returns it to the player's inventory. */
    void withdraw(Player player, int offerIndex);

    /** Marks this side as ready; completes the trade once both sides are. */
    void confirm(Player player);

    /**
     * Ends the trade without exchanging anything and gives every offered stack
     * back to whoever put it up. Safe to call more than once, and safe when the
     * player is in no trade at all - which is what lets the quit handler and the
     * window-close handler both call it unconditionally.
     */
    void cancel(Player player);

    /** Whatever could not fit back into a player's inventory is dropped at their feet rather than lost. */
    void returnItems(Player player, Iterable<ItemStack> items);

    void evict(UUID playerId);
}
