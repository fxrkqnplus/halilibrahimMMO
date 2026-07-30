package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V18 - creates the per-player permanent prestige reward tally (Faz 22).
 *
 * <p>One row per (player, reward), holding how many times that reward has been
 * picked, because a player prestiges up to ten times and may well choose the
 * same thing repeatedly. A count rather than a row per choice: nothing anywhere
 * needs to know <em>when</em> a reward was taken, only how much of it the
 * player is carrying.</p>
 */
public final class PrestigeRewardSchemaMigration implements Migration {
    @Override public int version() { return 18; }
    @Override public String description() { return "Prestij sonrasi secilen kalici odullerin tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_prestige_rewards (
                        player_id VARCHAR(36) NOT NULL,
                        reward_type VARCHAR(32) NOT NULL,
                        picked_count INTEGER NOT NULL,
                        PRIMARY KEY (player_id, reward_type)
                    )
                    """);
        }
    }
}
