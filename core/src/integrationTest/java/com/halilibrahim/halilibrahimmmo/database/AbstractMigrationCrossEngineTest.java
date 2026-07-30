package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Shared assertion body for "every shipped migration applies cleanly to a
 * real instance of this engine" - each concrete engine subclass only
 * supplies its own Testcontainers container + {@link DatabaseSettings}.
 * Every {@code dialect.autoIncrementPrimaryKeyType()}/upsert SQL fragment
 * in this codebase has, until this test class existed, only ever been
 * reasoned about structurally - never actually executed against a real
 * MySQL/MariaDB/PostgreSQL server.
 */
abstract class AbstractMigrationCrossEngineTest {

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available - skipping cross-engine integration tests"
        );
    }

    protected abstract DatabaseSettings settings();

    @Test
    void everyMigrationApplies(@TempDir Path tempDir) throws Exception {
        HikariDatabaseService database = IntegrationDatabaseSupport.provisionAndMigrate(settings(), tempDir);
        try {
            Set<Integer> appliedVersions = database.execute(connection -> {
                Set<Integer> versions = new TreeSet<>();
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT version FROM hmmo_schema_version")) {
                    while (result.next()) {
                        versions.add(result.getInt("version"));
                    }
                }
                return versions;
            }).get();

            assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), appliedVersions);
        } finally {
            database.disable();
        }
    }
}
