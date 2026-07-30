package com.halilibrahim.halilibrahimmmo.prestige;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for the permanent rewards a player has picked after prestiging. */
public final class PrestigeRewardRepository {

    private final DatabaseService database;

    public PrestigeRewardRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Map<PrestigeRewardType, Integer>> findAll(UUID playerId) {
        return database.execute(connection -> {
            Map<PrestigeRewardType, Integer> found = new EnumMap<>(PrestigeRewardType.class);
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT reward_type, picked_count FROM hmmo_player_prestige_rewards WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        int count = result.getInt("picked_count");
                        PrestigeRewardType.byName(result.getString("reward_type"))
                                .ifPresent(type -> found.merge(type, count, Integer::sum));
                    }
                }
            }
            return Map.copyOf(found);
        });
    }

    /**
     * Writes one reward's new total. UPDATE-then-INSERT, the same upsert idiom
     * every other repository in this codebase uses and the one the cross-engine
     * tests already cover on all four database families.
     */
    CompletableFuture<Void> save(UUID playerId, PrestigeRewardType type, int count) {
        return database.executeVoid(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                    UPDATE hmmo_player_prestige_rewards SET picked_count = ?
                    WHERE player_id = ? AND reward_type = ?
                    """)) {
                update.setInt(1, count);
                update.setString(2, playerId.toString());
                update.setString(3, type.name());
                if (update.executeUpdate() != 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_prestige_rewards (player_id, reward_type, picked_count)
                    VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, type.name());
                insert.setInt(3, count);
                insert.executeUpdate();
            }
        });
    }
}
