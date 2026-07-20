package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class TitleCosmeticSchemaMigrationTest {
    @Test void reservesThePermanentSixthSchemaVersion() {
        assertEquals(6, new TitleCosmeticSchemaMigration().version());
    }
}
