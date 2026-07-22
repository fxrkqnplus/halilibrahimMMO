package com.halilibrahim.halilibrahimmmo.prestige;

/** {@link PrestigeRules#eligibility(PrestigeEligibilityInput)}'s result: the status plus the per-condition breakdown behind it. */
public record PrestigeEligibility(PrestigeResult.Status status, PrestigeEligibilityReport report) {
}
