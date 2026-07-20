package com.halilibrahim.halilibrahimmmo.database.dialect;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;

/**
 * Hides the small but breaking SQL syntax differences between the four supported
 * engines (SQLite, MySQL, MariaDB, PostgreSQL) from every {@code Migration} and
 * repository class. New syntax differences discovered in later phases (upsert
 * statements, JSON columns, ...) get a new method here rather than scattering
 * {@code if (type == ...)} branches through business logic.
 */
public interface SqlDialect {

    DatabaseType type();

    /**
     * @return the full column definition (type + primary key + auto-increment
     * behaviour) for a surrogate numeric primary key, e.g.
     * {@code "BIGINT AUTO_INCREMENT PRIMARY KEY"} on MySQL/MariaDB.
     */
    String autoIncrementPrimaryKeyType();

    /**
     * @return the SQL expression used as a {@code DEFAULT} value for "now" on a
     * timestamp column.
     */
    String currentTimestampExpression();

    static SqlDialect forType(DatabaseType type) {
        return switch (type) {
            case SQLITE -> new SqliteDialect();
            case MYSQL, MARIADB -> new MySqlDialect(type);
            case POSTGRESQL -> new PostgreSqlDialect();
        };
    }
}
