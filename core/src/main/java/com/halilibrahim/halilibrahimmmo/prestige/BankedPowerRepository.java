package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_banked_power} table. */
public final class BankedPowerRepository {

    private final DatabaseService database;

    public BankedPowerRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Integer> find(UUID playerId) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT banked_power FROM hmmo_player_banked_power WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getInt("banked_power") : 0;
                }
            }
        });
    }

    CompletableFuture<Void> save(UUID playerId, int bankedPower) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_banked_power SET banked_power = ? WHERE player_id = ?
                    """)) {
                update.setInt(1, bankedPower);
                update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_banked_power (player_id, banked_power) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setInt(2, bankedPower);
                insert.executeUpdate();
            }
        });
    }
}
