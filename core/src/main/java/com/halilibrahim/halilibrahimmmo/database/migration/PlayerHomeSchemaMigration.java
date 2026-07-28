package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V12 - creates the one-home-per-player table behind {@code /ev}. */
public final class PlayerHomeSchemaMigration implements Migration {

    @Override
    public int version() {
        return 12;
    }

    @Override
    public String description() {
        return "Oyuncularin /ev komutuyla ayarladigi ev konumu tablosunu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_home (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        home_name VARCHAR(64) NOT NULL,
                        world_id VARCHAR(36) NOT NULL,
                        x DOUBLE PRECISION NOT NULL,
                        y DOUBLE PRECISION NOT NULL,
                        z DOUBLE PRECISION NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL
                    )
                    """);
        }
    }
}
