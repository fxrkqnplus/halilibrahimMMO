package com.halilibrahim.halilibrahimmmo.leaderboards;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** JDBC repository ranking players directly from persisted profile/skill/quest/collection tables. */
public final class LeaderboardRepository {

    private final DatabaseService database;

    public LeaderboardRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<List<LeaderboardEntry>> top(LeaderboardCategory category, int limit) {
        String sql = switch (category) {
            case GLOBAL_LEVEL -> """
                    SELECT player_name, global_level, global_experience
                    FROM hmmo_player_profiles
                    ORDER BY global_level DESC, global_experience DESC
                    LIMIT ?
                    """;
            case PRESTIGE -> """
                    SELECT player_name, prestige, global_level
                    FROM hmmo_player_profiles
                    ORDER BY prestige DESC, global_level DESC
                    LIMIT ?
                    """;
            case QUEST_COMPLETIONS -> """
                    SELECT p.player_name, COUNT(*), 0
                    FROM hmmo_player_quest_progress q
                    JOIN hmmo_player_profiles p ON p.player_id = q.player_id
                    WHERE q.completed = 1
                    GROUP BY q.player_id, p.player_name
                    ORDER BY 2 DESC
                    LIMIT ?
                    """;
            case COLLECTION_ENTRIES -> """
                    SELECT p.player_name, COUNT(*), 0
                    FROM hmmo_player_collection_entries c
                    JOIN hmmo_player_profiles p ON p.player_id = c.player_id
                    GROUP BY c.player_id, p.player_name
                    ORDER BY 2 DESC
                    LIMIT ?
                    """;
        };
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, limit);
                try (ResultSet result = statement.executeQuery()) {
                    return readEntries(result);
                }
            }
        });
    }

    CompletableFuture<List<LeaderboardEntry>> topSkill(String skillId, int limit) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT p.player_name, s.skill_level, s.skill_experience
                    FROM hmmo_player_skills s
                    JOIN hmmo_player_profiles p ON p.player_id = s.player_id
                    WHERE s.skill_id = ?
                    ORDER BY s.skill_level DESC, s.skill_experience DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, skillId);
                statement.setInt(2, limit);
                try (ResultSet result = statement.executeQuery()) {
                    return readEntries(result);
                }
            }
        });
    }

    private static List<LeaderboardEntry> readEntries(ResultSet result) throws SQLException {
        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        while (result.next()) {
            entries.add(new LeaderboardEntry(rank++, result.getString(1), result.getLong(2), result.getLong(3)));
        }
        return List.copyOf(entries);
    }
}
