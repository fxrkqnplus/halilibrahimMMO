package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V3 - creates persistent per-player skill progression. */
public final class SkillSchemaMigration implements Migration {

    @Override
    public int version() {
        return 3;
    }

    @Override
    public String description() {
        return "Oyuncu beceri ilerleme tablosunu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_skills (
                        player_id VARCHAR(36) NOT NULL,
                        skill_id VARCHAR(64) NOT NULL,
                        skill_level INTEGER NOT NULL,
                        skill_experience BIGINT NOT NULL,
                        updated_at TIMESTAMP NOT NULL DEFAULT %s,
                        PRIMARY KEY (player_id, skill_id)
                    )
                    """.formatted(dialect.currentTimestampExpression()));
        }
    }
}
