package com.halilibrahim.halilibrahimmmo.quests;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
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
}
