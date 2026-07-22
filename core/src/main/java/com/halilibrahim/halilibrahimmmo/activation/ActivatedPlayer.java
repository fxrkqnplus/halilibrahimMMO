package com.halilibrahim.halilibrahimmmo.activation;

import java.util.UUID;

/** One activated (registered) player - id/name pair, for admin tooling that must also reach offline players. */
public record ActivatedPlayer(UUID playerId, String playerName) {
}
