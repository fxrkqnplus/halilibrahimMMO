package com.halilibrahim.halilibrahimmmo.backpack;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_backpack} table. */
public final class BackpackRepository {

    private final DatabaseService database;

    public BackpackRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Optional<String>> find(UUID playerId) {
        return database.<Optional<String>>execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT contents FROM hmmo_player_backpack WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? Optional.ofNullable(result.getString("contents")) : Optional.empty();
                }
            }
        });
    }

    CompletableFuture<Integer> findUpgrades(UUID playerId) {
        return database.<Integer>execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT upgrades FROM hmmo_player_backpack_upgrades WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getInt("upgrades") : 0;
                }
            }
        });
    }

    CompletableFuture<Void> saveUpgrades(UUID playerId, int upgrades) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_backpack_upgrades SET upgrades = ? WHERE player_id = ?
                    """)) {
                update.setInt(1, upgrades);
                update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_backpack_upgrades (player_id, upgrades) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setInt(2, upgrades);
                insert.executeUpdate();
            }
        });
    }

    /** UPDATE-then-INSERT, the same portable upsert idiom every other repository here uses. */
    CompletableFuture<Void> save(UUID playerId, String contents) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_backpack SET contents = ? WHERE player_id = ?
                    """)) {
                update.setString(1, contents);
                update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_backpack (player_id, contents) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, contents);
                insert.executeUpdate();
            }
        });
    }
}
