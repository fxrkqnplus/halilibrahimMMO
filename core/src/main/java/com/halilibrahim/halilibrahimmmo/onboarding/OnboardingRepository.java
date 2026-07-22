package com.halilibrahim.halilibrahimmmo.onboarding;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC storage for the {@code hmmo_player_onboarding_tasks} INSERT-only membership table. */
public final class OnboardingRepository {

    private final DatabaseService database;

    public OnboardingRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Set<String>> findCompleted(UUID playerId) {
        return database.execute(connection -> {
            Set<String> found = new LinkedHashSet<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT task_id FROM hmmo_player_onboarding_tasks WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        found.add(result.getString("task_id"));
                    }
                }
            }
            return Set.copyOf(found);
        });
    }

    /** Callers must guarantee idempotency themselves - INSERT-only, same as {@code ActivationRepository}. */
    CompletableFuture<Void> markCompleted(UUID playerId, String taskId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_onboarding_tasks (player_id, task_id) VALUES (?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, taskId);
                insert.executeUpdate();
            }
        });
    }
}
