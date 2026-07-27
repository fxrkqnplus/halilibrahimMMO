package com.halilibrahim.halilibrahimmmo.home;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_home} table. */
public final class HomeRepository {

    private final DatabaseService database;

    public HomeRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Optional<PlayerHome>> find(UUID playerId) {
        return database.<Optional<PlayerHome>>execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT home_name, world_id, x, y, z, yaw, pitch FROM hmmo_player_home WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new PlayerHome(
                            playerId,
                            result.getString("home_name"),
                            UUID.fromString(result.getString("world_id")),
                            result.getDouble("x"),
                            result.getDouble("y"),
                            result.getDouble("z"),
                            result.getFloat("yaw"),
                            result.getFloat("pitch")
                    ));
                }
            }
        });
    }

    /** UPDATE-then-INSERT, the same portable upsert idiom every other repository here uses. */
    CompletableFuture<Void> save(PlayerHome home) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_home SET home_name = ?, world_id = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?
                    WHERE player_id = ?
                    """)) {
                update.setString(1, home.name());
                update.setString(2, home.worldId().toString());
                update.setDouble(3, home.x());
                update.setDouble(4, home.y());
                update.setDouble(5, home.z());
                update.setFloat(6, home.yaw());
                update.setFloat(7, home.pitch());
                update.setString(8, home.playerId().toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_home (player_id, home_name, world_id, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                insert.setString(1, home.playerId().toString());
                insert.setString(2, home.name());
                insert.setString(3, home.worldId().toString());
                insert.setDouble(4, home.x());
                insert.setDouble(5, home.y());
                insert.setDouble(6, home.z());
                insert.setFloat(7, home.yaw());
                insert.setFloat(8, home.pitch());
                insert.executeUpdate();
            }
        });
    }

    CompletableFuture<Void> delete(UUID playerId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    DELETE FROM hmmo_player_home WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.executeUpdate();
            }
        });
    }
}
