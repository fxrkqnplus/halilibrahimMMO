package com.halilibrahim.halilibrahimmmo.cosmetics;

/** Outcome of one cosmetic-equip attempt. */
public record CosmeticEquipResult(Status status, String cosmeticId) {
    public enum Status { EQUIPPED, ALREADY_EQUIPPED, NOT_UNLOCKED, UNKNOWN_COSMETIC }
}
