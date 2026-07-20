package com.halilibrahim.halilibrahimmmo.titles;

/** Outcome of one title-equip attempt. */
public record TitleEquipResult(Status status, String titleId) {
    public enum Status { EQUIPPED, ALREADY_EQUIPPED, NOT_UNLOCKED, UNKNOWN_TITLE }
}
