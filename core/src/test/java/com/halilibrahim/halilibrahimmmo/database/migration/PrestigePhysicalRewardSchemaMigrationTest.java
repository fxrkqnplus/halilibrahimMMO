package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins V20. A shipped migration's version is permanent - see {@link Migration}. */
final class PrestigePhysicalRewardSchemaMigrationTest {

    @Test
    void reservesVersionTwenty() {
        PrestigePhysicalRewardSchemaMigration migration = new PrestigePhysicalRewardSchemaMigration();
        assertEquals(20, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
