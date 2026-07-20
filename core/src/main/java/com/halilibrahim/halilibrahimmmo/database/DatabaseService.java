package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.core.PluginService;
import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Async, connection-pooled access to whichever database engine is configured in
 * {@code config.yml}. Every method returns a {@link CompletableFuture} completed on
 * a dedicated background thread pool - callers must never block the calling
 * thread, and must never touch the returned connection outside the given
 * callback.
 */
public interface DatabaseService extends PluginService {

    DatabaseType activeType();

    SqlDialect dialect();

    /**
     * Runs {@code action} on a pooled connection, on a background thread, and
     * completes the returned future with its result (or exceptionally, with the
     * thrown {@link SQLException} wrapped in a {@link java.util.concurrent.CompletionException}).
     */
    <T> CompletableFuture<T> execute(SqlFunction<T> action);

    /**
     * Same as {@link #execute(SqlFunction)} for actions with no result.
     */
    CompletableFuture<Void> executeVoid(SqlConsumer action);

    @FunctionalInterface
    interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
