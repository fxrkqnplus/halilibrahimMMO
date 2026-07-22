package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class OnboardingSchemaMigrationTest {
    @Test void reservesThePermanentTenthSchemaVersion() {
        assertEquals(10, new OnboardingSchemaMigration().version());
    }
}
