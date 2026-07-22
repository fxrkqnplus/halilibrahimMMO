package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V10 - creates the one-time onboarding tutorial-nudge membership table. */
public final class OnboardingSchemaMigration implements Migration {
    @Override public int version() { return 10; }
    @Override public String description() { return "Baslangic (onboarding) gorev tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_onboarding_tasks (
                        player_id VARCHAR(36) NOT NULL,
                        task_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_id, task_id)
                    )
                    """);
        }
    }
}
