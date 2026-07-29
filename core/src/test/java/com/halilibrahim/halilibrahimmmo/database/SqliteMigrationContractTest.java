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
import com.halilibrahim.halilibrahimmmo.database.migration.BackpackSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestPinSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.TalentGrantSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.WealthSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.PrestigeBankedPowerSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestAssignmentSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.QuestSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.SkillSchemaMigration;
import com.halilibrahim.halilibrahimmmo.database.migration.TitleCosmeticSchemaMigration;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The first test in this project to run every migration against a real
 * (file-based) connection - a fast, Docker-free control check that the
 * shared migration chain itself is sound, independent of engine.
 */
final class SqliteMigrationContractTest {

    @Test
    void everyMigrationAppliesToARealSqliteDatabase(@TempDir Path tempDir) throws Exception {
        DatabaseSettings settings = new DatabaseSettings(
                DatabaseType.SQLITE, "integration-test.db", "", 0, "", "", "", false, 1, 1, 5000L, 30000L, 600000L
        );
        PluginLogger logger = new PluginLogger(LoggerFactory.getLogger("HalilIbrahimMMO-Test"), () -> true);
        HikariDatabaseService database = new HikariDatabaseService(settings, tempDir, logger);
        database.enable();
        try {
            List<Migration> migrations = List.of(
                    new CoreSchemaMigration(), new PlayerProfileSchemaMigration(), new SkillSchemaMigration(),
                    new ClassSchemaMigration(), new QuestSchemaMigration(), new TitleCosmeticSchemaMigration(),
                    new ClassProgressSchemaMigration(), new PlayerActivationSchemaMigration(), new QuestAssignmentSchemaMigration(),
                    new OnboardingSchemaMigration(), new PrestigeBankedPowerSchemaMigration(),
                    new PlayerHomeSchemaMigration(), new WealthSchemaMigration(), new QuestPinSchemaMigration(), new BackpackSchemaMigration(), new TalentGrantSchemaMigration()
            );
            new MigrationRunner(database, logger, migrations).runPendingMigrations().get();

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

            assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), appliedVersions);
        } finally {
            database.disable();
        }
    }
}
