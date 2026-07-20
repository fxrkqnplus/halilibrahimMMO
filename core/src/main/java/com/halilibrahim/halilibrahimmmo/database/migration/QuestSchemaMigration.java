package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** V5 - creates quest progress, collection entry and adventure journal tables. */
public final class QuestSchemaMigration implements Migration {

    @Override
    public int version() {
        return 5;
    }

    @Override
    public String description() {
        return "Gorev ilerlemesi, koleksiyon ogeleri ve macera gunlugu tablolarini olusturur";
    }

    @Override
    public void migrate(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_quest_progress (
                        player_id VARCHAR(36) NOT NULL,
                        quest_id VARCHAR(64) NOT NULL,
                        progress INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        period_end_millis BIGINT NOT NULL,
                        PRIMARY KEY (player_id, quest_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_collection_entries (
                        player_id VARCHAR(36) NOT NULL,
                        collection_id VARCHAR(64) NOT NULL,
                        entry_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_id, collection_id, entry_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS hmmo_player_journal_entries (
                        id %s,
                        player_id VARCHAR(36) NOT NULL,
                        entry_type VARCHAR(32) NOT NULL,
                        reference_id VARCHAR(64) NOT NULL,
                        reference_name VARCHAR(128) NOT NULL,
                        experience_awarded BIGINT NOT NULL,
                        created_at_millis BIGINT NOT NULL
                    )
                    """.formatted(dialect.autoIncrementPrimaryKeyType()));
        }
    }
}
