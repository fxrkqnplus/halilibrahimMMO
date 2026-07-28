package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V14 - creates the per-player sidebar quest pin table (Phase 17).
 *
 * <p>Mirrors {@code hmmo_player_quest_assignments}' shape exactly, including
 * the comma-joined id column: a pin set is at most three ids and is always read
 * and written whole, so a row per pin would buy nothing but joins.</p>
 */
public final class QuestPinSchemaMigration implements Migration {
    @Override public int version() { return 14; }
    @Override public String description() { return "Oyuncunun panele sabitledigi gorevlerin tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_quest_pins (
                        player_id VARCHAR(36) NOT NULL,
                        quest_type VARCHAR(16) NOT NULL,
                        pinned_quest_ids TEXT NOT NULL,
                        PRIMARY KEY (player_id, quest_type)
                    )
                    """);
        }
    }
}
