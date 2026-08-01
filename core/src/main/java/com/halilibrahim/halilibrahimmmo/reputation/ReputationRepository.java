package com.halilibrahim.halilibrahimmmo.reputation;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_wealth} table. */
public final class ReputationRepository {

    private final DatabaseService database;

    public ReputationRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Long> find(UUID playerId) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT reputation_points FROM hmmo_player_wealth WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getLong("reputation_points") : 0L;
                }
            }
        });
    }

    /** UPDATE-then-INSERT, the same upsert idiom every other repository here uses. */
    CompletableFuture<Void> save(UUID playerId, long reputationPoints) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_wealth SET reputation_points = ? WHERE player_id = ?
                    """)) {
                update.setLong(1, reputationPoints);
                update.setString(2, playerId.toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_wealth (player_id, reputation_points) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setLong(2, reputationPoints);
                insert.executeUpdate();
            }
        });
    }
}
