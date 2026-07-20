package com.halilibrahim.halilibrahimmmo.database.dialect;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;

/**
 * Shared dialect for MySQL and MariaDB - their DDL syntax is identical for
 * everything HalilIbrahimMMO needs (auto-increment keys, timestamp defaults).
 */
public final class MySqlDialect implements SqlDialect {

    private final DatabaseType type;

    public MySqlDialect(DatabaseType type) {
        if (type != DatabaseType.MYSQL && type != DatabaseType.MARIADB) {
            throw new IllegalArgumentException("MySqlDialect only supports MYSQL or MARIADB, got: " + type);
        }
        this.type = type;
    }

    @Override
    public DatabaseType type() {
        return type;
    }

    @Override
    public String autoIncrementPrimaryKeyType() {
        return "BIGINT AUTO_INCREMENT PRIMARY KEY";
    }

    @Override
    public String currentTimestampExpression() {
        return "CURRENT_TIMESTAMP";
    }
}
