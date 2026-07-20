package com.halilibrahim.halilibrahimmmo.collections;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for durable, per-player obtained collection entries. */
public final class CollectionRepository {

    private final DatabaseService database;

    public CollectionRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Map<String, Set<String>>> findAll(UUID playerId) {
        return database.execute(connection -> {
            Map<String, Set<String>> found = new LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT collection_id, entry_id FROM hmmo_player_collection_entries WHERE player_id = ?
                    """)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        found.computeIfAbsent(result.getString("collection_id"), ignored -> new LinkedHashSet<>())
                                .add(result.getString("entry_id"));
                    }
                }
            }
            Map<String, Set<String>> copy = new LinkedHashMap<>();
            found.forEach((collectionId, entries) -> copy.put(collectionId, Set.copyOf(entries)));
            return Map.copyOf(copy);
        });
    }

    /**
     * Persists one newly obtained entry. Callers must guarantee (via their own
     * per-player-per-collection serialization) that this is only ever called once
     * per player/entry pair - there is no uniqueness handling here beyond the
     * table's composite primary key.
     */
    CompletableFuture<Void> insertEntry(UUID playerId, String collectionId, String entryId) {
        return database.executeVoid(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_collection_entries (player_id, collection_id, entry_id) VALUES (?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, collectionId);
                insert.setString(3, entryId);
                insert.executeUpdate();
            }
        });
    }
}
