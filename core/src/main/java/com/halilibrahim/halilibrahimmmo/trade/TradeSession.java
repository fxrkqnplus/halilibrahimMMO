package com.halilibrahim.halilibrahimmmo.trade;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One live two-sided trade.
 *
 * <p><strong>Offered items live here, not in an inventory.</strong> The moment
 * a player offers something it is taken out of their inventory and held by the
 * session; the trade screen only ever renders copies of it. That is what makes
 * the whole feature safe against the classic duplication bug - there is exactly
 * one authoritative copy of every offered stack at every instant, and the
 * screen is a view of it rather than the place it is stored. It also means a
 * crash, a kick or a closed window has one obvious correct response
 * ({@link #drain}), instead of having to reconcile two inventories.</p>
 *
 * <p>Every mutation happens on the main thread from the GUI click handler or a
 * command, so the plain {@link ArrayList}s need no synchronisation.</p>
 */
public final class TradeSession {

    /** Most stacks one side may put on the table - the trade screen's offer grid. */
    public static final int MAXIMUM_OFFER_SIZE = 12;

    private final UUID firstPlayerId;
    private final UUID secondPlayerId;
    private final List<ItemStack> firstOffer = new ArrayList<>();
    private final List<ItemStack> secondOffer = new ArrayList<>();
    private boolean firstConfirmed;
    private boolean secondConfirmed;
    private boolean finished;

    public TradeSession(UUID firstPlayerId, UUID secondPlayerId) {
        this.firstPlayerId = firstPlayerId;
        this.secondPlayerId = secondPlayerId;
    }

    public UUID firstPlayerId() {
        return firstPlayerId;
    }

    public UUID secondPlayerId() {
        return secondPlayerId;
    }

    public UUID partnerOf(UUID playerId) {
        return firstPlayerId.equals(playerId) ? secondPlayerId : firstPlayerId;
    }

    public boolean involves(UUID playerId) {
        return firstPlayerId.equals(playerId) || secondPlayerId.equals(playerId);
    }

    public List<ItemStack> offerOf(UUID playerId) {
        return firstPlayerId.equals(playerId) ? firstOffer : secondOffer;
    }

    public boolean hasConfirmed(UUID playerId) {
        return firstPlayerId.equals(playerId) ? firstConfirmed : secondConfirmed;
    }

    public boolean bothConfirmed() {
        return firstConfirmed && secondConfirmed;
    }

    /**
     * Any change to what is on the table clears <em>both</em> confirmations.
     * Without that, one side could confirm, wait for the other to confirm, and
     * pull their own goods off the table in between.
     */
    public void clearConfirmations() {
        firstConfirmed = false;
        secondConfirmed = false;
    }

    public void confirm(UUID playerId) {
        if (firstPlayerId.equals(playerId)) {
            firstConfirmed = true;
        } else {
            secondConfirmed = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void markFinished() {
        finished = true;
    }

    /** Hands back one side's offered stacks and empties it, so nothing can be returned twice. */
    public List<ItemStack> drain(UUID playerId) {
        List<ItemStack> offer = offerOf(playerId);
        List<ItemStack> copy = List.copyOf(offer);
        offer.clear();
        return copy;
    }
}
