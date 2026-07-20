package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.DatabaseType;
import com.halilibrahim.halilibrahimmmo.database.IntegrationDatabaseSupport;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
final class TitleRepositoryMariaDbIntegrationTest extends AbstractTitleRepositoryCrossEngineTest {

    @Container
    private static final MariaDBContainer CONTAINER = new MariaDBContainer(DockerImageName.parse("mariadb:11.4"));

    @Override
    protected DatabaseSettings settings() {
        return IntegrationDatabaseSupport.settingsFor(DatabaseType.MARIADB, CONTAINER);
    }
}
