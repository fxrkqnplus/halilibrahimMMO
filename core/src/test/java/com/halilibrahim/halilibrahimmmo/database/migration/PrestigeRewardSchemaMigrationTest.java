package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Pins the prestige reward migration's version - a shipped migration's number must never move. */
final class PrestigeRewardSchemaMigrationTest {

    @Test
    void reservesVersionEighteen() {
        PrestigeRewardSchemaMigration migration = new PrestigeRewardSchemaMigration();
        assertEquals(18, migration.version());
        assertFalse(migration.description().isBlank());
    }
}
