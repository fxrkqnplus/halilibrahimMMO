package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V4 - creates persistent class-talent allocations and class-change cooldowns. */
public final class ClassSchemaMigration implements Migration {
    @Override public int version() { return 4; }
    @Override public String description() { return "Sinif yetenekleri ve degisim bekleme tablolarini olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_class_talents (
                        player_id VARCHAR(36) NOT NULL,
                        class_id VARCHAR(64) NOT NULL,
                        talent_id VARCHAR(64) NOT NULL,
                        talent_rank INTEGER NOT NULL,
                        PRIMARY KEY (player_id, class_id, talent_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_class_cooldowns (
                        player_id VARCHAR(36) PRIMARY KEY,
                        ready_at_millis BIGINT NOT NULL
                    )
                    """);
        }
    }
}
