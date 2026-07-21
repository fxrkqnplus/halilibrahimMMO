package com.halilibrahim.halilibrahimmmo.abilities;

/**
 * How an armed/buffed ability's remaining time is determined. {@code SURE}
 * (duration) abilities simply run for {@code activeMillis} - today's original
 * behavior. {@code ENERJI} (energy-drain) abilities additionally debit energy
 * on every qualifying action (currently: a PvE hit landed while a
 * {@link AbilityEffectType#DAMAGE_BUFF} is active) and end the moment energy
 * runs out, with {@code activeMillis} kept as a hard fallback expiry so the
 * ability can never run indefinitely even if energy is never spent.
 */
public enum AbilityActivationMode {
    SURE,
    ENERJI
}
