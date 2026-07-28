package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the Zenginlik Puanı migration's version - a shipped migration's number must never move. */
final class WealthSchemaMigrationTest {

    @Test
    void reservesVersionThirteen() {
        WealthSchemaMigration migration = new WealthSchemaMigration();
        assertEquals(13, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
