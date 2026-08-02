package com.halilibrahim.halilibrahimmmo.boss;

/**
 * What a world boss is currently trying to do.
 *
 * <p>The brain picks exactly one of these per behaviour tick and then acts on
 * it alone. Keeping the choice explicit is what stops a boss from fleeing and
 * chasing in the same half second - the previous behaviour loop ran every rule
 * it had in sequence, so a creature at low health teleported away and then
 * immediately blinked back toward the player who had hurt it.</p>
 */
public enum BossPhase {

    /** Nobody is close: walk the map, and pick on whatever is grazing nearby. */
    ROAMING,
    /** A player is in range and this is a fight. */
    HUNTING,
    /** A village is in reach and there is no player to answer for it. */
    RAIDING,
    /** Hurt badly enough to leave: get distance, at any cost. */
    FLEEING,
    /** Far enough away to mend, and unwilling to come back before it has. */
    RECOVERING
}
