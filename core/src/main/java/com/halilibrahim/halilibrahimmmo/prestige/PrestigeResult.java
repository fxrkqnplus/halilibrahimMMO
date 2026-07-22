package com.halilibrahim.halilibrahimmmo.prestige;

import java.util.Optional;

/** Outcome of a confirmed prestige attempt. {@code report} is empty for {@code PRESTIGED}/{@code MAXIMUM_REACHED}. */
public record PrestigeResult(Status status, int prestige, Optional<PrestigeEligibilityReport> report) {
    public enum Status { PRESTIGED, REQUIREMENTS_NOT_MET, MAXIMUM_REACHED }
}
