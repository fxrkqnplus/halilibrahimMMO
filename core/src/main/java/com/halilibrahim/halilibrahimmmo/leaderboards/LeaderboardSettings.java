package com.halilibrahim.halilibrahimmmo.leaderboards;

/** Immutable parsed {@code leaderboards.yml} snapshot. */
public record LeaderboardSettings(int entryLimit, long refreshIntervalMillis) {
}
