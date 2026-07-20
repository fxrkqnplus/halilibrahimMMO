package com.halilibrahim.halilibrahimmmo.titles;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.HikariDatabaseService;
import com.halilibrahim.halilibrahimmmo.database.IntegrationDatabaseSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link TitleRepository}'s two SQL idioms against a real engine:
 * the INSERT-only membership pattern (shared by {@code CollectionRepository}/
 * {@code CosmeticRepository}) and the equip UPDATE-then-INSERT upsert -
 * the newest-phase repository, never before executed against anything but
 * SQLite.
 */
abstract class AbstractTitleRepositoryCrossEngineTest {

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(
                DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available - skipping cross-engine integration tests"
        );
    }

    protected abstract DatabaseSettings settings();

    @Test
    void unlockMembershipAccumulates(@TempDir Path tempDir) throws Exception {
        HikariDatabaseService database = IntegrationDatabaseSupport.provisionAndMigrate(settings(), tempDir);
        try {
            TitleRepository repository = new TitleRepository(database);
            UUID playerId = UUID.randomUUID();

            repository.insertUnlock(playerId, "gezgin").get();
            repository.insertUnlock(playerId, "efsane").get();

            Set<String> unlocked = repository.findUnlockedIds(playerId).get();
            assertEquals(Set.of("gezgin", "efsane"), unlocked);
        } finally {
            database.disable();
        }
    }

    @Test
    void equipUpsertUpdatesRatherThanDuplicating(@TempDir Path tempDir) throws Exception {
        HikariDatabaseService database = IntegrationDatabaseSupport.provisionAndMigrate(settings(), tempDir);
        try {
            TitleRepository repository = new TitleRepository(database);
            UUID playerId = UUID.randomUUID();

            repository.saveEquipped(playerId, "gezgin").get();
            repository.saveEquipped(playerId, "efsane").get();

            String equipped = repository.findEquipped(playerId).get();
            assertEquals("efsane", equipped);
        } finally {
            database.disable();
        }
    }
}
