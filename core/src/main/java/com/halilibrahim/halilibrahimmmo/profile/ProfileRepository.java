package com.halilibrahim.halilibrahimmmo.profile;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository that keeps all profile access on DatabaseService's worker pool. */
public final class ProfileRepository {

    private final DatabaseService database;

    public ProfileRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Optional<PlayerProfile>> find(UUID playerId) {
        return database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT player_name, global_level, global_experience, prestige, selected_class
                    FROM hmmo_player_profiles WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(read(playerId, resultSet));
                }
            }
        });
    }

    CompletableFuture<Void> save(PlayerProfile profile) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_profiles
                    SET player_name = ?, global_level = ?, global_experience = ?, prestige = ?, selected_class = ?,
                        updated_at = %s
                    WHERE player_id = ?
                    """.formatted(database.dialect().currentTimestampExpression()))) {
                bind(update, profile, false);
                update.setString(6, profile.playerId().toString());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_profiles
                    (player_id, player_name, global_level, global_experience, prestige, selected_class)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                insert.setString(1, profile.playerId().toString());
                bind(insert, profile, true);
                insert.executeUpdate();
            }
        });
    }

    CompletableFuture<Void> incrementStatistic(UUID playerId, String key, long amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_statistics
                    SET statistic_value = statistic_value + ?, updated_at = %s
                    WHERE player_id = ? AND statistic_key = ?
                    """.formatted(database.dialect().currentTimestampExpression()))) {
                update.setLong(1, amount);
                update.setString(2, playerId.toString());
                update.setString(3, key);
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_statistics (player_id, statistic_key, statistic_value) VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, key);
                insert.setLong(3, amount);
                insert.executeUpdate();
            }
        });
    }

    private PlayerProfile read(UUID playerId, ResultSet resultSet) throws SQLException {
        return new PlayerProfile(
                playerId,
                resultSet.getString("player_name"),
                resultSet.getInt("global_level"),
                resultSet.getLong("global_experience"),
                resultSet.getInt("prestige"),
                resultSet.getString("selected_class")
        );
    }

    private void bind(PreparedStatement statement, PlayerProfile profile, boolean insert) throws SQLException {
        int offset = insert ? 1 : 0;
        statement.setString(1 + offset, profile.playerName());
        statement.setInt(2 + offset, profile.globalLevel());
        statement.setLong(3 + offset, profile.globalExperience());
        statement.setInt(4 + offset, profile.prestige());
        statement.setString(5 + offset, profile.selectedClass());
    }
}
