package com.halilibrahim.halilibrahimmmo.api.titles;

import java.util.UUID;

/** Immutable, external-safe view of one player's currently equipped title. */
public record TitleSnapshot(UUID playerId, String titleId, String displayName) {
}
