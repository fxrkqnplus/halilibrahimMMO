package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V11 - creates the per-player banked Güç table (Phase 12: prestige must never lower Güç). */
public final class PrestigeBankedPowerSchemaMigration implements Migration {
    @Override public int version() { return 11; }
    @Override public String description() { return "Prestij sonrasi Guc'un dusmemesi icin biriken guc tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_banked_power (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        banked_power INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        }
    }
}
