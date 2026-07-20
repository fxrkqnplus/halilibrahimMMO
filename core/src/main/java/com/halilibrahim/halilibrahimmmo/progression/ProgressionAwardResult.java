package com.halilibrahim.halilibrahimmmo.progression;

import com.halilibrahim.halilibrahimmmo.api.profile.PlayerProfileSnapshot;

/** Outcome of one XP award attempt, including whether it crossed a level boundary. */
public record ProgressionAwardResult(PlayerProfileSnapshot snapshot, boolean leveledUp) {
}
