package com.halilibrahim.halilibrahimmmo.database.migration;

import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A single, ordered, irreversible schema change. Every migration ever shipped
 * must keep a permanently unique {@link #version()} - never renumber or delete an
 * old migration, even if the schema it created later gets altered by a newer one.
 */
public interface Migration {

    /**
     * @return this migration's version number. Applied in ascending order and
     * recorded in {@code hmmo_schema_version} so it never runs twice.
     */
    int version();

    /**
     * @return a short, human-readable description logged when this migration runs,
     * e.g. {@code "Eklenti meta veri tablosunu oluşturur"}.
     */
    String description();

    /**
     * Applies this migration. Runs inside a transaction managed by
     * {@link MigrationRunner} - do not call {@link Connection#commit()} or
     * {@link Connection#rollback()} here.
     */
    void migrate(Connection connection, SqlDialect dialect) throws SQLException;
}
