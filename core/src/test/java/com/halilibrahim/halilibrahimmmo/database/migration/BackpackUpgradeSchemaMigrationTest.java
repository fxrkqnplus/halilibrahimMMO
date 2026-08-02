package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Pins V19. A shipped migration's version is permanent - renumbering one would
 * silently re-run or skip it on every server that already applied it.
 */
final class BackpackUpgradeSchemaMigrationTest {

    @Test
    void reservesVersionNineteen() {
        BackpackUpgradeSchemaMigration migration = new BackpackUpgradeSchemaMigration();
        assertEquals(19, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
