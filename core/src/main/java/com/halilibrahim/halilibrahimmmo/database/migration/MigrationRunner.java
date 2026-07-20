package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.DatabaseService;
import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Applies every pending {@link Migration} exactly once, in ascending version
 * order, tracked in a {@code hmmo_schema_version} bookkeeping table.
 *
 * <p>Runs entirely through {@link DatabaseService#executeVoid(DatabaseService.SqlConsumer)},
 * i.e. on the database's own background thread pool - the plugin's main thread
 * never touches JDBC directly. {@code HalilIbrahimMMOPlugin#onEnable()} waits for
 * the returned future to complete before enabling any subsystem that depends on
 * the schema being ready, which is the one deliberate, documented case where
 * plugin startup blocks on a background task.</p>
 */
public final class MigrationRunner {

    private static final String SCHEMA_TABLE = "hmmo_schema_version";

    private final DatabaseService databaseService;
    private final PluginLogger logger;
    private final List<Migration> migrations;

    public MigrationRunner(DatabaseService databaseService, PluginLogger logger, List<Migration> migrations) {
        this.databaseService = databaseService;
        this.logger = logger;
        this.migrations = migrations.stream()
                .sorted(Comparator.comparingInt(Migration::version))
                .toList();
    }

    public CompletableFuture<Void> runPendingMigrations() {
        return databaseService.executeVoid(connection -> applyPending(connection, databaseService.dialect()));
    }

    private void applyPending(Connection connection, SqlDialect dialect) throws SQLException {
        ensureSchemaTable(connection, dialect);
        Set<Integer> applied = loadAppliedVersions(connection);

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (Migration migration : migrations) {
                if (applied.contains(migration.version())) {
                    continue;
                }

                logger.info("Veritabani gocu uygulaniyor: v" + migration.version() + " - " + migration.description());
                migration.migrate(connection, dialect);
                recordApplied(connection, migration);
                connection.commit();
            }
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void ensureSchemaTable(Connection connection, SqlDialect dialect) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        version INTEGER PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at TIMESTAMP NOT NULL DEFAULT %s
                    )
                    """.formatted(SCHEMA_TABLE, dialect.currentTimestampExpression()));
        }
    }

    private Set<Integer> loadAppliedVersions(Connection connection) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM " + SCHEMA_TABLE)) {
            while (resultSet.next()) {
                versions.add(resultSet.getInt("version"));
            }
        }
        return versions;
    }

    private void recordApplied(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + SCHEMA_TABLE + " (version, description) VALUES (?, ?)")) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.executeUpdate();
        }
    }
}
