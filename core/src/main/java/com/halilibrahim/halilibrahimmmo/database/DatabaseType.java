package com.halilibrahim.halilibrahimmmo.database;

/**
 * Every database engine HalilIbrahimMMO can persist data to. Configured through
 * {@code config.yml} ({@code veritabani.tur}); SQLite is the zero-configuration
 * default so the plugin works out of the box with no external server.
 */
public enum DatabaseType {
    SQLITE,
    MYSQL,
    MARIADB,
    POSTGRESQL
}
