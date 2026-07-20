package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V2 - creates durable player profiles and extensible statistic counters. */
public final class PlayerProfileSchemaMigration implements Migration {

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String description() {
        return "Oyuncu profilleri ve istatistik tablolarini olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_profiles (
                        player_id VARCHAR(36) PRIMARY KEY,
                        player_name VARCHAR(16) NOT NULL,
                        global_level INTEGER NOT NULL,
                        global_experience BIGINT NOT NULL,
                        prestige INTEGER NOT NULL DEFAULT 0,
                        selected_class VARCHAR(64) NOT NULL DEFAULT '',
                        created_at TIMESTAMP NOT NULL DEFAULT %s,
                        updated_at TIMESTAMP NOT NULL DEFAULT %s
                    )
                    """.formatted(dialect.currentTimestampExpression(), dialect.currentTimestampExpression()));
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_statistics (
                        player_id VARCHAR(36) NOT NULL,
                        statistic_key VARCHAR(64) NOT NULL,
                        statistic_value BIGINT NOT NULL,
                        updated_at TIMESTAMP NOT NULL DEFAULT %s,
                        PRIMARY KEY (player_id, statistic_key)
                    )
                    """.formatted(dialect.currentTimestampExpression()));
        }
    }
}
