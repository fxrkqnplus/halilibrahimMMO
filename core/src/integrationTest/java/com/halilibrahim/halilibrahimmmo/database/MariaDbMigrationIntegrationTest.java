package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class MariaDbMigrationIntegrationTest extends AbstractMigrationCrossEngineTest {

    @Container
    private static final MariaDBContainer CONTAINER = new MariaDBContainer(DockerImageName.parse("mariadb:11.4"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.MARIADB, CONTAINER);
    }
}
