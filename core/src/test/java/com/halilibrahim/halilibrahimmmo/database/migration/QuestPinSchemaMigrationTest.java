package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the sidebar quest-pin migration's version - a shipped migration's number must never move. */
final class QuestPinSchemaMigrationTest {

    @Test
    void reservesVersionFourteen() {
        QuestPinSchemaMigration migration = new QuestPinSchemaMigration();
        assertEquals(14, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
