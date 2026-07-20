package com.halilibrahim.halilibrahimmmo.leaderboards;

/**
 * One ranked row of a leaderboard. {@code secondaryValue} carries a
 * supporting metric where one exists (experience alongside a level ranking);
 * boards with a single metric leave it at {@code 0}.
 */
public record LeaderboardEntry(int rank, String playerName, long primaryValue, long secondaryValue) {
}
