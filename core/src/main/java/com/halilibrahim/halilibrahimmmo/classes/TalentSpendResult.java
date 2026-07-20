package com.halilibrahim.halilibrahimmmo.classes;

/** Outcome of one talent-rank purchase. */
public record TalentSpendResult(Status status, String talentId, int rank, int availablePoints) {
    public enum Status { SPENT, NO_CLASS, UNKNOWN_TALENT, MAXIMUM_RANK, NO_POINTS }
}
