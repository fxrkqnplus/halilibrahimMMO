package com.halilibrahim.halilibrahimmmo.api.cosmetics;

import java.util.UUID;

/** Immutable, external-safe view of one player's currently equipped cosmetic. */
public record CosmeticSnapshot(UUID playerId, String cosmeticId, String displayName) {
}
