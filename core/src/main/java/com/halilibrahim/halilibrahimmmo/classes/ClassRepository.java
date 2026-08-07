package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Asynchronous JDBC storage for talent ranks, class-change cooldowns and per-class level tracks. */
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
            Map<String, ClassLevelProgress> classLevels = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT class_id, class_level, class_experience FROM hmmo_player_class_progress WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String classId = result.getString("class_id");
                        classLevels.put(classId, new ClassLevelProgress(classId, result.getInt("class_level"), result.getLong("class_experience")));
                    }
                }
            }
            return new PlayerClassProgress(playerId, copy(ranks), readyAt, Map.copyOf(classLevels));
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

    /**
     * Wipes every talent rank one player holds in one class.
     *
     * <p>Written as a delete rather than as a run of zero-rank rows: a rank of
     * zero and no rank at all mean the same thing everywhere that reads this
     * table, and rows nobody can see are rows that quietly grow.</p>
     */
    CompletableFuture<Void> clearRanks(UUID playerId, String classId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement delete = connection.prepareStatement("""
                    DELETE FROM hmmo_player_class_talents WHERE player_id = ? AND class_id = ?
                    """)) {
                delete.setString(1, playerId.toString());
                delete.setString(2, classId);
                delete.executeUpdate();
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

    CompletableFuture<Void> saveClassLevel(UUID playerId, String classId, int level, long experience) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_class_progress SET class_level = ?, class_experience = ? WHERE player_id = ? AND class_id = ?
                    """)) {
                update.setInt(1, level); update.setLong(2, experience); update.setString(3, playerId.toString()); update.setString(4, classId);
                if (update.executeUpdate() != 0) return;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_class_progress (player_id, class_id, class_level, class_experience) VALUES (?, ?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString()); insert.setString(2, classId); insert.setInt(3, level); insert.setLong(4, experience); insert.executeUpdate();
            }
        });
    }

    /** The extra talent points an admin has handed this player, per pool. */
    CompletableFuture<Map<TalentPointCategory, Integer>> loadGrants(UUID playerId) {
        return database.execute(connection -> {
            Map<TalentPointCategory, Integer> grants = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT category, amount FROM hmmo_player_talent_grants WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        try {
                            grants.put(TalentPointCategory.valueOf(result.getString("category")), result.getInt("amount"));
                        } catch (IllegalArgumentException exception) {
                            // A pool removed from the code after a grant was
                            // written: the row stays, it simply stops counting.
                        }
                    }
                }
            }
            return Map.copyOf(grants);
        });
    }

    CompletableFuture<Void> saveGrant(UUID playerId, TalentPointCategory category, int amount) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_talent_grants SET amount = ? WHERE player_id = ? AND category = ?
                    """)) {
                update.setInt(1, amount); update.setString(2, playerId.toString()); update.setString(3, category.name());
                if (update.executeUpdate() != 0) return;
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_talent_grants (player_id, category, amount) VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString()); insert.setString(2, category.name()); insert.setInt(3, amount);
                insert.executeUpdate();
            }
        });
    }

    private static Map<String, Map<String, Integer>> copy(Map<String, Map<String, Integer>> source) {
        Map<String, Map<String, Integer>> copied = new LinkedHashMap<>();
        source.forEach((classId, ranks) -> copied.put(classId, Map.copyOf(ranks)));
        return Map.copyOf(copied);
    }
}
