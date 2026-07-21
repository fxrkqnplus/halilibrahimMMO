package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V7 - creates the per (player, class) level/XP track, independent of talent ranks. */
public final class ClassProgressSchemaMigration implements Migration {
    @Override public int version() { return 7; }
    @Override public String description() { return "Sinif seviyesi/XP takip tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_class_progress (
                        player_id VARCHAR(36) NOT NULL,
                        class_id VARCHAR(64) NOT NULL,
                        class_level INTEGER NOT NULL,
                        class_experience BIGINT NOT NULL,
                        PRIMARY KEY (player_id, class_id)
                    )
                    """);
        }
    }
}
