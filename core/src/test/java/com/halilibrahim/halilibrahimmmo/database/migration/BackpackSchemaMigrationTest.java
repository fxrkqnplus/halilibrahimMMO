package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the cloud-chest migration's version - a shipped migration's number must never move. */
final class BackpackSchemaMigrationTest {

    @Test
    void reservesVersionFifteen() {
        BackpackSchemaMigration migration = new BackpackSchemaMigration();
        assertEquals(15, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
