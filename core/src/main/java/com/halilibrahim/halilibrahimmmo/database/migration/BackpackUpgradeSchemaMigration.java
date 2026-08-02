package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V19 - how many times a player has widened their cloud chest.
 *
 * <p>A count rather than a row per purchase, for the same reason
 * {@code hmmo_player_prestige_rewards} is: nobody will ever ask <em>when</em> a
 * backpack was widened, only how wide it is now.</p>
 *
 * <p><strong>A table of its own rather than a column on
 * {@code hmmo_player_backpack}.</strong> That table holds the contents and is
 * written on every close; this is written twice in a player's whole life and is
 * read on join. Bolting a capacity onto the contents row would mean the
 * capacity is only known once the contents have been fetched, which is exactly
 * backwards - the size of the window has to be decided before it is opened.</p>
 */
public final class BackpackUpgradeSchemaMigration implements Migration {

    @Override
    public int version() {
        return 19;
    }

    @Override
    public String description() {
        return "Oyuncularin bulut sandigi genisletme sayisini saklayan tabloyu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_backpack_upgrades (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        upgrades INTEGER NOT NULL
                    )
                    """);
        }
    }
}
