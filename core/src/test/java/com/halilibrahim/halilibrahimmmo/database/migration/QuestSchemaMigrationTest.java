package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestSchemaMigrationTest {
    @Test void reservesThePermanentFifthSchemaVersion() {
        assertEquals(5, new QuestSchemaMigration().version());
    }
}
