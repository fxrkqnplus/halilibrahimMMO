package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class QuestAssignmentSchemaMigrationTest {
    @Test void reservesThePermanentNinthSchemaVersion() {
        assertEquals(9, new QuestAssignmentSchemaMigration().version());
    }
}
