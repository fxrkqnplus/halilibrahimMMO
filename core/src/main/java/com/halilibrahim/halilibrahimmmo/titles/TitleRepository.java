package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for unlocked-title membership and the currently equipped title. */
public final class TitleRepository {

    private final DatabaseService database;

    public TitleRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Set<String>> findUnlockedIds(UUID playerId) {
        return database.execute(connection -> {
            Set<String> ids = new LinkedHashSet<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT title_id FROM hmmo_player_unlocked_titles WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        ids.add(result.getString("title_id"));
                    }
                }
            }
            return Set.copyOf(ids);
        });
    }

    /**
     * Callers must guarantee (via their own per-player serialization) that
     * this is only ever called once per player/title pair - there is no
     * uniqueness handling here beyond the table's composite primary key.
     */
    CompletableFuture<Void> insertUnlock(UUID playerId, String titleId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_unlocked_titles (player_id, title_id) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, titleId);
                insert.executeUpdate();
            }
        });
    }

    CompletableFuture<String> findEquipped(UUID playerId) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT title_id FROM hmmo_player_equipped_title WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getString("title_id") : "";
                }
            }
        });
    }

    CompletableFuture<Void> saveEquipped(UUID playerId, String titleId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_equipped_title SET title_id = ? WHERE player_id = ?
                    """)) {
                update.setString(1, titleId);
                update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_equipped_title (player_id, title_id) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, titleId);
                insert.executeUpdate();
            }
        });
    }
}
