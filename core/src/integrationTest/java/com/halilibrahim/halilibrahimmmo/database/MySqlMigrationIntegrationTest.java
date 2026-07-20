package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class MySqlMigrationIntegrationTest extends AbstractMigrationCrossEngineTest {

    @Container
    private static final MySQLContainer CONTAINER = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.MYSQL, CONTAINER);
    }
}
