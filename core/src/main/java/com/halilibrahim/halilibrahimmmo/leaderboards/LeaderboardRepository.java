package com.halilibrahim.halilibrahimmmo.leaderboards;

import com.halilibrahim.halilibrahimmmo.api.leaderboards.LeaderboardCategory;
import com.halilibrahim.halilibrahimmmo.database.DatabaseService;
import com.halilibrahim.halilibrahimmmo.skills.SkillService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/** JDBC repository ranking players directly from persisted profile/skill/quest/collection tables. */
public final class LeaderboardRepository {

    private final DatabaseService database;
    private final SkillService skills;

    public LeaderboardRepository(DatabaseService database, SkillService skills) {
        this.database = database;
        this.skills = skills;
    }

    CompletableFuture<List<LeaderboardEntry>> top(LeaderboardCategory category, int limit) {
        if (category == LeaderboardCategory.GLOBAL_LEVEL) {
            return topGlobalLevel(limit);
        }
        String sql = switch (category) {
            case GLOBAL_LEVEL -> throw new IllegalStateException("Handled above.");
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

    /**
     * Güç has no column of its own (Phase 11) - it is the sum of every
     * configured skill's level plus any Güç permanently banked across past
     * prestiges (Phase 12, {@code hmmo_player_banked_power}), so this board
     * is a {@code LEFT JOIN}/{@code SUM} aggregate rather than a plain column
     * read. Prestiged players always outrank non-prestiged ones regardless of
     * raw level, per the resolved design: {@code ORDER BY prestige DESC,
     * total_level DESC}. The skill id list is bound dynamically from the live
     * configured catalogue rather than assuming a fixed arity.
     */
    private CompletableFuture<List<LeaderboardEntry>> topGlobalLevel(int limit) {
        List<String> skillIds = skills.definitions().stream().map(definition -> definition.id()).toList();
        String placeholders = skillIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT p.player_name, COALESCE(SUM(s.skill_level), 0) + COALESCE(b.banked_power, 0) AS total_level, p.prestige
                FROM hmmo_player_profiles p
                LEFT JOIN hmmo_player_skills s ON s.player_id = p.player_id AND s.skill_id IN (%s)
                LEFT JOIN hmmo_player_banked_power b ON b.player_id = p.player_id
                GROUP BY p.player_id, p.player_name, p.prestige, b.banked_power
                ORDER BY p.prestige DESC, total_level DESC
                LIMIT ?
                """.formatted(placeholders);
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (String skillId : skillIds) {
                    statement.setString(index++, skillId);
                }
                statement.setInt(index, limit);
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
