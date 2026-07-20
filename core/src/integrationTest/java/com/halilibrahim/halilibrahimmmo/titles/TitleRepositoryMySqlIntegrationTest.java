package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.DatabaseType;
import com.halilibrahim.halilibrahimmmo.database.IntegrationDatabaseSupport;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class TitleRepositoryMySqlIntegrationTest extends AbstractTitleRepositoryCrossEngineTest {

    @Container
    private static final MySQLContainer CONTAINER = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.MYSQL, CONTAINER);
    }
}
