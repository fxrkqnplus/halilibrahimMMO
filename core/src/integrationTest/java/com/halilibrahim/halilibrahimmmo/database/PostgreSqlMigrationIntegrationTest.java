package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class PostgreSqlMigrationIntegrationTest extends AbstractMigrationCrossEngineTest {

    @Container
    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.POSTGRESQL, CONTAINER);
    }
}
