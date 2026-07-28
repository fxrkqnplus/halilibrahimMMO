package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for durable per-player, per-quest progress and reset windows. */
public final class QuestRepository {

    private final DatabaseService database;

    public QuestRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Map<String, QuestProgress>> findAll(UUID playerId) {
        return database.execute(connection -> {
            Map<String, QuestProgress> found = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT quest_id, progress, completed, period_end_millis
                    FROM hmmo_player_quest_progress WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String questId = result.getString("quest_id");
                        found.put(questId, new QuestProgress(
                                playerId,
                                questId,
                                result.getInt("progress"),
                                result.getInt("completed") != 0,
                                result.getLong("period_end_millis")
                        ));
                    }
                }
            }
            return Map.copyOf(found);
        });
    }

    CompletableFuture<Void> save(QuestProgress progress) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_quest_progress
                    SET progress = ?, completed = ?, period_end_millis = ?
                    WHERE player_id = ? AND quest_id = ?
                    """)) {
                update.setInt(1, progress.progress());
                update.setInt(2, progress.completed() ? 1 : 0);
                update.setLong(3, progress.periodEndMillis());
                update.setString(4, progress.playerId().toString());
                update.setString(5, progress.questId());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_quest_progress (player_id, quest_id, progress, completed, period_end_millis)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                insert.setString(1, progress.playerId().toString());
                insert.setString(2, progress.questId());
                insert.setInt(3, progress.progress());
                insert.setInt(4, progress.completed() ? 1 : 0);
                insert.setLong(5, progress.periodEndMillis());
                insert.executeUpdate();
            }
        });
    }

    /** {@code assignedQuestIds} is a comma-joined list of quest ids - see {@link QuestAssignment}. */
    CompletableFuture<Optional<QuestAssignment>> findAssignment(UUID playerId, QuestType type) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT assigned_quest_ids, period_end_millis FROM hmmo_player_quest_assignments
                    WHERE player_id = ? AND quest_type = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, type.name());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new QuestAssignment(
                            Set.of(result.getString("assigned_quest_ids").split(",")),
                            result.getLong("period_end_millis")
                    ));
                }
            }
        });
    }

    CompletableFuture<Void> saveAssignment(UUID playerId, QuestType type, QuestAssignment assignment) {
        String joined = String.join(",", assignment.questIds());
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_quest_assignments SET assigned_quest_ids = ?, period_end_millis = ?
                    WHERE player_id = ? AND quest_type = ?
                    """)) {
                update.setString(1, joined);
                update.setLong(2, assignment.periodEndMillis());
                update.setString(3, playerId.toString());
                update.setString(4, type.name());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_quest_assignments (player_id, quest_type, assigned_quest_ids, period_end_millis)
                    VALUES (?, ?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, type.name());
                insert.setString(3, joined);
                insert.setLong(4, assignment.periodEndMillis());
                insert.executeUpdate();
            }
        });
    }

    /** The quest ids this player has pinned to their sidebar for one section. */
    CompletableFuture<Set<String>> findPins(UUID playerId, QuestType type) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT pinned_quest_ids FROM hmmo_player_quest_pins
                    WHERE player_id = ? AND quest_type = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, type.name());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Set.<String>of();
                    }
                    String raw = result.getString("pinned_quest_ids");
                    return raw.isBlank() ? Set.<String>of() : Set.of(raw.split(","));
                }
            }
        });
    }

    CompletableFuture<Void> savePins(UUID playerId, QuestType type, Set<String> questIds) {
        String joined = String.join(",", questIds);
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_quest_pins SET pinned_quest_ids = ?
                    WHERE player_id = ? AND quest_type = ?
                    """)) {
                update.setString(1, joined);
                update.setString(2, playerId.toString());
                update.setString(3, type.name());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_quest_pins (player_id, quest_type, pinned_quest_ids)
                    VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, type.name());
                insert.setString(3, joined);
                insert.executeUpdate();
            }
        });
    }
}
