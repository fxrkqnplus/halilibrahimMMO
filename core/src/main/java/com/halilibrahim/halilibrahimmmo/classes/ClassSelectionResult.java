package com.halilibrahim.halilibrahimmmo.classes;

/** Outcome of one class-selection attempt. */
public record ClassSelectionResult(Status status, String classId, long remainingMillis) {
    public enum Status { SELECTED, ALREADY_SELECTED, UNKNOWN_CLASS, COOLDOWN }
}
