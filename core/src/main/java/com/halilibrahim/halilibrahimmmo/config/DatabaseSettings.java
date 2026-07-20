package com.halilibrahim.halilibrahimmmo.config;

import com.halilibrahim.halilibrahimmmo.database.DatabaseType;

/**
 * Immutable snapshot of the {@code veritabani} section of {@code config.yml}.
 *
 * @param type                 which engine to connect to
 * @param sqliteFileName       file name used when {@code type == SQLITE}, relative to the plugin data folder
 * @param host                 server host, ignored for SQLite
 * @param port                 server port, ignored for SQLite
 * @param databaseName         schema/database name, ignored for SQLite
 * @param username             ignored for SQLite
 * @param password             ignored for SQLite
 * @param useSsl               whether to require an SSL/TLS connection, ignored for SQLite
 * @param maxPoolSize          maximum HikariCP pool size
 * @param minimumIdle          minimum idle connections kept warm in the pool
 * @param connectionTimeoutMs  milliseconds to wait for a connection before failing
 * @param idleTimeoutMs        milliseconds an idle connection may sit before being retired
 * @param maxLifetimeMs        milliseconds before a connection is retired regardless of activity
 */
public record DatabaseSettings(
        DatabaseType type,
        String sqliteFileName,
        String host,
        int port,
        String databaseName,
        String username,
        String password,
        boolean useSsl,
        int maxPoolSize,
        int minimumIdle,
        long connectionTimeoutMs,
        long idleTimeoutMs,
        long maxLifetimeMs
) {
}
