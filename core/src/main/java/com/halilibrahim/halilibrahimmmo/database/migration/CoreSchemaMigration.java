package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V1 - creates the plugin-wide key/value metadata table.
 *
 * <p>This is the very first migration ever applied and exists purely to prove the
 * migration pipeline works end-to-end across all four supported engines. Phase 2
 * adds the real player-data schema (profiles, statistics, ...) as new migrations
 * on top of this one - this class itself must never be modified once released.</p>
 */
public final class CoreSchemaMigration implements Migration {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Eklenti meta veri tablosunu olusturur (hmmo_plugin_meta)";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_plugin_meta (
                        meta_key VARCHAR(64) PRIMARY KEY,
                        meta_value VARCHAR(255) NOT NULL
                    )
                    """);
        }
    }
}
