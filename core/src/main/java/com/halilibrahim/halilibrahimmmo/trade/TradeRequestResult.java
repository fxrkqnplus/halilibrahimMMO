package com.halilibrahim.halilibrahimmmo.trade;

/** Why a {@code /takas} request was accepted or refused, so the command layer owns every message. */
public enum TradeRequestResult {

    SENT,
    /** The sender has not reached the class level to start a trade (and has never prestiged). */
    LOCKED,
    /** The other player has not reached the class level to be traded with (and has never prestiged). */
    PARTNER_LOCKED,
    /** No such player is online. */
    PARTNER_OFFLINE,
    /** A player cannot trade with themselves. */
    SELF,
    /** One of the two is already in a trade or already has a pending request. */
    BUSY,
    /** The other player has not activated the MMO system yet. */
    PARTNER_NOT_ACTIVATED
}
