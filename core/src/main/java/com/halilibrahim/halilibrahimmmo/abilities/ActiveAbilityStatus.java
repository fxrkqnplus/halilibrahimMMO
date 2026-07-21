package com.halilibrahim.halilibrahimmmo.abilities;

/**
 * A player's currently active (armed or buffed) ability, exposed synchronously
 * so action-bar countdown feedback can poll it without an async round-trip.
 */
public record ActiveAbilityStatus(String abilityId, long expiresAtMillis) {
}
