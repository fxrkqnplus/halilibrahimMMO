package com.halilibrahim.halilibrahimmmo.database.dialect;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;

public final class PostgreSqlDialect implements SqlDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String autoIncrementPrimaryKeyType() {
        return "BIGSERIAL PRIMARY KEY";
    }

    @Override
    public String currentTimestampExpression() {
        return "CURRENT_TIMESTAMP";
    }
}
