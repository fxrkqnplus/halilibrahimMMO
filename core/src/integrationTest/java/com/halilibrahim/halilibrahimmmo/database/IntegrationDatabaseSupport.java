package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.migration.ClassProgressSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.ClassSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.CoreSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.Migration;
import com.halilibrahim.halilibrahimmmo.database.migration.MigrationRunner;
import com.halilibrahim.halilibrahimmmo.database.migration.OnboardingSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerActivationSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerProfileSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PlayerHomeSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestPinSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.WealthSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PrestigeBankedPowerSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestAssignmentSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.SkillSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.TitleCosmeticSchemaMigration;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Shared provisioning for cross-engine integration tests: builds a
 * {@link DatabaseSettings} from any of the three Testcontainers JDBC
 * modules used in this project (they all share the {@link JdbcDatabaseContainer}
 * interface) and runs every shipped migration against it, exactly the way
 * {@code HalilIbrahimMMOPlugin#onEnable()} does at real server startup.
 */
public final class IntegrationDatabaseSupport {

    private IntegrationDatabaseSupport() {
        throw new UnsupportedOperationException("IntegrationDatabaseSupport is a static utility class");
    }

    public static DatabaseSettings settingsFor(DatabaseType type, JdbcDatabaseContainer<?> container) {
        return new DatabaseSettings(
                type, "", container.getHost(), container.getFirstMappedPort(), container.getDatabaseName(),
                container.getUsername(), container.getPassword(), false, 4, 1, 5000L, 30000L, 600000L
        );
    }

    public static List<Migration> allMigrations() {
        return List.of(
                new CoreSchemaMigration(), new PlayerProfileSchemaMigration(), new SkillSchemaMigration(),
                new ClassSchemaMigration(), new QuestSchemaMigration(), new TitleCosmeticSchemaMigration(),
                new ClassProgressSchemaMigration(), new PlayerActivationSchemaMigration(), new QuestAssignmentSchemaMigration(),
                new OnboardingSchemaMigration(), new PrestigeBankedPowerSchemaMigration(),
                new PlayerHomeSchemaMigration(), new WealthSchemaMigration(), new QuestPinSchemaMigration()
        );
    }

    public static HikariDatabaseService provisionAndMigrate(DatabaseSettings settings, Path tempDir) throws ExecutionException, InterruptedException {
        PluginLogger logger = new PluginLogger(LoggerFactory.getLogger("HalilIbrahimMMO-IT"), () -> true);
        HikariDatabaseService database = new HikariDatabaseService(settings, tempDir, logger);
        database.enable();
        new MigrationRunner(database, logger, allMigrations()).runPendingMigrations().get();
        return database;
    }
}
