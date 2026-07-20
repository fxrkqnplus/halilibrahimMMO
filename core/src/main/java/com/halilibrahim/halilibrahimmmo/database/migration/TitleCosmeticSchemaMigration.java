package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V6 - creates unlocked/equipped title and cosmetic tables. */
public final class TitleCosmeticSchemaMigration implements Migration {

    @Override
    public int version() {
        return 6;
    }

    @Override
    public String description() {
        return "Unvan ve kozmetik acma/kusanma tablolarini olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_unlocked_titles (
                        player_id VARCHAR(36) NOT NULL,
                        title_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_id, title_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_equipped_title (
                        player_id VARCHAR(36) PRIMARY KEY,
                        title_id VARCHAR(64) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_unlocked_cosmetics (
                        player_id VARCHAR(36) NOT NULL,
                        cosmetic_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_id, cosmetic_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_equipped_cosmetic (
                        player_id VARCHAR(36) PRIMARY KEY,
                        cosmetic_id VARCHAR(64) NOT NULL
                    )
                    """);
        }
    }
}
