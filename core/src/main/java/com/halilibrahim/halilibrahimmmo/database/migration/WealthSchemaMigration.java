package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V13 - creates the per-player Zenginlik Puanı table (Phase 17). */
public final class WealthSchemaMigration implements Migration {
    @Override public int version() { return 13; }
    @Override public String description() { return "Oyuncu basina Zenginlik Puani tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_wealth (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        wealth_points BIGINT NOT NULL DEFAULT 0
                    )
                    """);
        }
    }
}
