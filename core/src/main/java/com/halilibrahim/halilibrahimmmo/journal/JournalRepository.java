package com.halilibrahim.halilibrahimmmo.journal;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** JDBC repository for the append-only adventure journal. */
public final class JournalRepository {

    private final DatabaseService database;

    public JournalRepository(DatabaseService database) {
        this.database = database;
    }

    CompletableFuture<Void> insert(UUID playerId, JournalEntryType type, String referenceId, String referenceName, long experienceAwarded) {
        return database.executeVoid(connection -> {
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO hmmo_player_journal_entries
                    (player_id, entry_type, reference_id, reference_name, experience_awarded, created_at_millis)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                insert.setString(1, playerId.toString());
                insert.setString(2, type.name());
                insert.setString(3, referenceId);
                insert.setString(4, referenceName);
                insert.setLong(5, experienceAwarded);
                insert.setLong(6, System.currentTimeMillis());
                insert.executeUpdate();
            }
        });
    }

    CompletableFuture<List<JournalEntry>> findRecent(UUID playerId, int limit) {
        return database.execute(connection -> {
            List<JournalEntry> entries = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, entry_type, reference_id, reference_name, experience_awarded, created_at_millis
                    FROM hmmo_player_journal_entries WHERE player_id = ?
                    ORDER BY created_at_millis DESC, id DESC
                    LIMIT ?
                    """)) {
                statement.setString(1, playerId.toString());
                statement.setInt(2, limit);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        entries.add(new JournalEntry(
                                result.getLong("id"),
                                playerId,
                                JournalEntryType.valueOf(result.getString("entry_type")),
                                result.getString("reference_id"),
                                result.getString("reference_name"),
                                result.getLong("experience_awarded"),
                                result.getLong("created_at_millis")
                        ));
                    }
                }
            }
            return List.copyOf(entries);
        });
    }
}
