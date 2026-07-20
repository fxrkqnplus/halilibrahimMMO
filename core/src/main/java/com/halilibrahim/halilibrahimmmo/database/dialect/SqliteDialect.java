package com.halilibrahim.halilibrahimmmo.database.dialect;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;

public final class SqliteDialect implements SqlDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLITE;
    }

    @Override
    public String autoIncrementPrimaryKeyType() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public String currentTimestampExpression() {
        return "CURRENT_TIMESTAMP";
    }
}
