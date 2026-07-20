package com.halilibrahim.halilibrahimmmo.prestige;

/** Outcome of a confirmed prestige attempt. */
public record PrestigeResult(Status status, int prestige) {
    public enum Status { PRESTIGED, LEVEL_REQUIRED, MAXIMUM_REACHED }
}
