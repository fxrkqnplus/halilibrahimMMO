package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V8 - creates the {@code /mmo aktif} onboarding-activation membership table. */
public final class PlayerActivationSchemaMigration implements Migration {
    @Override public int version() { return 8; }
    @Override public String description() { return "Oyuncu aktivasyon (/mmo aktif) tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_activation (
                        player_id VARCHAR(36) PRIMARY KEY,
                        activated_at_millis BIGINT NOT NULL
                    )
                    """);
        }
    }
}
