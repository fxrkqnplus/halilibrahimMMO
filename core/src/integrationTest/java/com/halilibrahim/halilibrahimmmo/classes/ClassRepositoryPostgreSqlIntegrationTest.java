package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.DatabaseType;
import com.halilibrahim.halilibrahimmmo.database.IntegrationDatabaseSupport;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class ClassRepositoryPostgreSqlIntegrationTest extends AbstractClassRepositoryCrossEngineTest {

    @Container
    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.POSTGRESQL, CONTAINER);
    }
}
