package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * V17 - creates the table of finished quests a player has taken off their panel
 * (Faz 22).
 *
 * <p>An INSERT-only membership table, the same shape
 * {@code hmmo_player_unlocked_titles} established: the row's existence is the
 * whole fact, and there is nothing about it to update. Deliberately <em>not</em>
 * folded into {@code hmmo_player_quest_pins}' comma-joined column - a pin set is
 * capped at three and always rewritten whole, while a player can dismiss every
 * completed quest they ever finish.</p>
 */
public final class QuestDismissalSchemaMigration implements Migration {
    @Override public int version() { return 17; }
    @Override public String description() { return "Oyuncunun panelden kaldirdigi tamamlanmis gorevlerin tablosunu olusturur"; }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_quest_dismissals (
                        player_id VARCHAR(36) NOT NULL,
                        quest_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_id, quest_id)
                    )
                    """);
        }
    }
}
