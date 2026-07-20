package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous JDBC storage for talent ranks and persistent class-change cooldowns. */
public final class ClassRepository {
    private final DatabaseService database;

    public ClassRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<PlayerClassProgress> load(UUID playerId) {
        return database.execute(connection -> {
            Map<String, Map<String, Integer>> ranks = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT class_id, talent_id, talent_rank FROM hmmo_player_class_talents WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        ranks.computeIfAbsent(result.getString("class_id"), ignored -> new LinkedHashMap<>())
                                .put(result.getString("talent_id"), result.getInt("talent_rank"));
                    }
                }
            }
            long readyAt = 0L;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT ready_at_millis FROM hmmo_player_class_cooldowns WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        readyAt = result.getLong("ready_at_millis");
                    }
                }
            }
            return new PlayerClassProgress(playerId, copy(ranks), readyAt);
        });
    }

    CompletableFuture<Void> saveRank(UUID playerId, String classId, String talentId, int rank) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_class_talents SET talent_rank = ? WHERE player_id = ? AND class_id = ? AND talent_id = ?
                    """)) {
                update.setInt(1, rank); update.setString(2, playerId.toString()); update.setString(3, classId); update.setString(4, talentId);
                if (update.executeUpdate() != 0) return;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_class_talents (player_id, class_id, talent_id, talent_rank) VALUES (?, ?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString()); insert.setString(2, classId); insert.setString(3, talentId); insert.setInt(4, rank); insert.executeUpdate();
            }
        });
    }

    CompletableFuture<Void> saveCooldown(UUID playerId, long readyAtMillis) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_class_cooldowns SET ready_at_millis = ? WHERE player_id = ?
                    """)) {
                update.setLong(1, readyAtMillis); update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) return;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_class_cooldowns (player_id, ready_at_millis) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString()); insert.setLong(2, readyAtMillis); insert.executeUpdate();
            }
        });
    }

    private static Map<String, Map<String, Integer>> copy(Map<String, Map<String, Integer>> source) {
        Map<String, Map<String, Integer>> copied = new LinkedHashMap<>();
        source.forEach((classId, ranks) -> copied.put(classId, Map.copyOf(ranks)));
        return Map.copyOf(copied);
    }
}
