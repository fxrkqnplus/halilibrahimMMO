package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V9 - creates the per (player, quest type) random daily/weekly quest pool assignment table. */
public final class QuestAssignmentSchemaMigration implements Migration {
    @Override public int version() { return 9; }
    @Override public String description() { return "Gunluk/haftalik gorev havuzu atama tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_quest_assignments (
                        player_id VARCHAR(36) NOT NULL,
                        quest_type VARCHAR(16) NOT NULL,
                        assigned_quest_ids VARCHAR(512) NOT NULL,
                        period_end_millis BIGINT NOT NULL,
                        PRIMARY KEY (player_id, quest_type)
                    )
                    """);
        }
    }
}
