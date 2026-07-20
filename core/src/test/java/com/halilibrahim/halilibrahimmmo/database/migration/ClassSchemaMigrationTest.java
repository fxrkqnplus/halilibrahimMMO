package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClassSchemaMigrationTest {
    @Test void reservesThePermanentFourthSchemaVersion() {
        assertEquals(4, new ClassSchemaMigration().version());
    }
}
