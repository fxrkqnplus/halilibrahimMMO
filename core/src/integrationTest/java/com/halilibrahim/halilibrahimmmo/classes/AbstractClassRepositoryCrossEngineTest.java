package com.halilibrahim.halilibrahimmmo.classes;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.HikariDatabaseService;
import com.halilibrahim.halilibrahimmmo.database.IntegrationDatabaseSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link ClassRepository}'s UPDATE-then-INSERT-on-0-rows upsert
 * idiom against a real engine - the same shape {@code ProfileRepository},
 * {@code TitleRepository} and {@code CosmeticRepository} all reuse, and
 * never before executed against anything but SQLite.
 */
abstract class AbstractClassRepositoryCrossEngineTest {

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available - skipping cross-engine integration tests"
        );
    }

    protected abstract DatabaseSettings settings();

    @Test
    void cooldownUpsertUpdatesRatherThanDuplicating(@TempDir Path tempDir) throws Exception {
        HikariDatabaseService database = IntegrationDatabaseSupport.provisionAndMigrate(settings(), tempDir);
        try {
            ClassRepository repository = new ClassRepository(database);
            UUID playerId = UUID.randomUUID();

            repository.saveCooldown(playerId, 1000L).get();
            repository.saveCooldown(playerId, 2000L).get();

            PlayerClassProgress progress = repository.load(playerId).get();
            assertEquals(2000L, progress.classChangeReadyAtMillis());
        } finally {
            database.disable();
        }
    }

    @Test
    void talentRankUpsertUpdatesRatherThanDuplicating(@TempDir Path tempDir) throws Exception {
        HikariDatabaseService database = IntegrationDatabaseSupport.provisionAndMigrate(settings(), tempDir);
        try {
            ClassRepository repository = new ClassRepository(database);
            UUID playerId = UUID.randomUUID();

            repository.saveRank(playerId, "tabiat-varisi", "guc", 1).get();
            repository.saveRank(playerId, "tabiat-varisi", "guc", 3).get();

            PlayerClassProgress progress = repository.load(playerId).get();
            assertEquals(3, progress.talentRanks().get("tabiat-varisi").get("guc"));
        } finally {
            database.disable();
        }
    }
}
