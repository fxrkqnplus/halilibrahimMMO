package com.halilibrahim.halilibrahimmmo.home;

/**
 * Why a {@code /ev} action succeeded or was refused, so the command layer owns
 * every message and the service owns none.
 */
public enum HomeActionResult {

    /** The action went through. */
    SUCCESS,
    /** The player has not reached the class level (and has never prestiged). */
    LOCKED,
    /** No home has been set yet. */
    NOT_SET,
    /** Homes exist in the overworld only - the Nether and the End are excluded on both ends of the trip. */
    FORBIDDEN_WORLD,
    /** The home's world is no longer loaded on this server. */
    WORLD_MISSING,
    /** The requested home name is empty or longer than {@link HomeService#MAXIMUM_NAME_LENGTH}. */
    INVALID_NAME
}
