package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the dismissed-quest migration's version - a shipped migration's number must never move. */
final class QuestDismissalSchemaMigrationTest {

    @Test
    void reservesVersionSeventeen() {
        QuestDismissalSchemaMigration migration = new QuestDismissalSchemaMigration();
        assertEquals(17, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
