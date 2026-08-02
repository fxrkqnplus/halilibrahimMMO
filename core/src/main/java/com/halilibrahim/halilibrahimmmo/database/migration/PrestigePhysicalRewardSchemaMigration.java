package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V20 - how many physical prestige crates a player has taken.
 *
 * <p>A single count rather than a row per crate, and deliberately <em>not</em>
 * which crates: the items went into an inventory the moment they were claimed
 * and the plugin has no further business tracking them. All this table has to
 * answer is "does this player still owe themselves a crate", which is
 * {@code prestige - taken} - derived the same way the permanent reward's
 * pending choice is, so a crash mid-menu or an admin editing a rank cannot put
 * the two out of step.</p>
 */
public final class PrestigePhysicalRewardSchemaMigration implements Migration {

    @Override
    public int version() {
        return 20;
    }

    @Override
    public String description() {
        return "Oyuncularin aldigi fiziksel prestij odulu sayisini saklayan tabloyu olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_prestige_physical (
                        player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                        taken_count INTEGER NOT NULL
                    )
                    """);
        }
    }
}
