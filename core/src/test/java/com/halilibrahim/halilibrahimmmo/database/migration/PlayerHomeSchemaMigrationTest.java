package com.halilibrahim.halilibrahimmmo.database.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PlayerHomeSchemaMigrationTest {

    /** A shipped migration's version is permanent - renumbering it would re-run or skip it on live databases. */
    @Test
    void reservesVersionTwelve() {
        assertEquals(12, new PlayerHomeSchemaMigration().version());
        assertFalse(new PlayerHomeSchemaMigration().description().isBlank());
    }
}
