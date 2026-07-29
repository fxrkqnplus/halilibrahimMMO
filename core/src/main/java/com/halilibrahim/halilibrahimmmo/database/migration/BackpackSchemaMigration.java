package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V15 - creates the one-backpack-per-player table behind {@code /çanta}.
 *
 * <p>Contents are stored as Base64 text rather than as a binary column
 * deliberately: this plugin ships on four database engines whose BLOB
 * handling and JDBC driver behaviour genuinely differ, while a long text
 * column behaves identically on all of them. The cost is roughly a third more
 * bytes for a table that holds at most 27 stacks per player.</p>
 */
public final class BackpackSchemaMigration implements Migration {

    @Override
    public int version() {
        return 15;
    }

    @Override
    public String description() {
        return "Oyuncularin /canta bulut sandiginin icerigini saklayan tabloyu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_backpack (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        contents TEXT NOT NULL
                    )
                    """);
        }
    }
}
