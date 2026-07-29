package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the admin talent-grant migration's version - a shipped migration's number must never move. */
final class TalentGrantSchemaMigrationTest {

    @Test
    void reservesVersionSixteen() {
        TalentGrantSchemaMigration migration = new TalentGrantSchemaMigration();
        assertEquals(16, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
