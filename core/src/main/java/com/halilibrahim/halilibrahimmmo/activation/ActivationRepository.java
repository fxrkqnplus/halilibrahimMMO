package com.halilibrahim.halilibrahimmmo.activation;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_activation} INSERT-only membership table. */
public final class ActivationRepository {

    private final DatabaseService database;

    public ActivationRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Boolean> isActivated(UUID playerId) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT 1 FROM hmmo_player_activation WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    return result.next();
                }
            }
        });
    }

    /**
     * Callers must guarantee (via their own idempotency check) that this is
     * only ever called once per player - there is no uniqueness handling
     * here beyond the table's primary key, matching every other INSERT-only
     * membership repository in this codebase (e.g. {@code TitleRepository}).
     */
    CompletableFuture<Void> activate(UUID playerId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_activation (player_id, activated_at_millis) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setLong(2, System.currentTimeMillis());
                insert.executeUpdate();
            }
        });
    }

    /** Every activated player's id/name, including offline ones - for the admin all-players panel. */
    CompletableFuture<List<ActivatedPlayer>> findAllActivated() {
        return database.execute(connection -> {
            List<ActivatedPlayer> found = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT p.player_id, p.player_name
                    FROM hmmo_player_activation a
                    JOIN hmmo_player_profiles p ON p.player_id = a.player_id
                    ORDER BY p.player_name
                    """)) {
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        found.add(new ActivatedPlayer(UUID.fromString(result.getString("player_id")), result.getString("player_name")));
                    }
                }
            }
            return List.copyOf(found);
        });
    }
}
