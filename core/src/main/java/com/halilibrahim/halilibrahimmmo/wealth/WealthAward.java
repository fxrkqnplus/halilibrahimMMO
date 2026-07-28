package com.halilibrahim.halilibrahimmmo.wealth;

/**
 * One completed Zenginlik Puanı award.
 *
 * @param granted           how many points were actually added
 * @param newTotal          the player's total afterwards
 * @param crossedMilestone  the milestone this award pushed the player past, or
 *                          {@code 0} if it crossed none - the only thing that
 *                          turns a routine award into an announcement
 */
public record WealthAward(long granted, long newTotal, long crossedMilestone) {

    public boolean reachedMilestone() {
        return crossedMilestone > 0;
    }
}
