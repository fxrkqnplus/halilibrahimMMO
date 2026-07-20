package com.halilibrahim.halilibrahimmmo.api.leaderboards;

/**
 * Immutable, external-safe view of one ranked row of a leaderboard.
 *
 * <p>{@code secondaryValue} carries a supporting metric where one exists
 * (experience alongside a level ranking); boards with a single metric leave
 * it at {@code 0}.</p>
 */
public record LeaderboardEntrySnapshot(int rank, String playerName, long primaryValue, long secondaryValue) {
}
