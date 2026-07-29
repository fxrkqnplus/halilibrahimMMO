package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V16 - stores talent points an admin handed a player, per pool.
 *
 * <p>A separate number rather than a nudge to the derived budget, because the
 * budget genuinely <em>is</em> derived - it recomputes from Güç and prestige
 * every time it is read, so anything written into it would evaporate on the
 * next level-up. Keeping the grant beside it means the two add up cleanly and
 * an admin correction survives everything the player does afterwards, including
 * a prestige.</p>
 */
public final class TalentGrantSchemaMigration implements Migration {

    @Override
    public int version() {
        return 16;
    }

    @Override
    public String description() {
        return "Yoneticinin oyuncuya verdigi ek yetenek puanlarini saklayan tabloyu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_talent_grants (
                        player_id VARCHAR(36) NOT NULL,
                        category VARCHAR(16) NOT NULL,
                        amount INTEGER NOT NULL,
                        PRIMARY KEY (player_id, category)
                    )
                    """);
        }
    }
}
