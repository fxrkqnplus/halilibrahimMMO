package com.halilibrahim.halilibrahimmmo.database;

import com.halilibrahim.halilibrahimmmo.config.DatabaseSettings;
import com.halilibrahim.halilibrahimmmo.database.dialect.SqlDialect;
import com.halilibrahim.halilibrahimmmo.logging.PluginLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link DatabaseService} backed by a HikariCP connection pool. Supports SQLite,
 * MySQL, MariaDB and PostgreSQL through a single implementation; the only
 * per-engine differences (driver class name, JDBC URL shape) are resolved once in
 * {@link #enable()}.
 *
 * <p>All query execution happens on a dedicated, bounded, daemon thread pool -
 * never on the Bukkit main thread and never on Paper's region schedulers, which
 * are reserved for gameplay logic.</p>
 */
public final class HikariDatabaseService implements DatabaseService {

    private final DatabaseSettings settings;
    private final SqlDialect dialect;
    private final Path dataFolder;
    private final PluginLogger logger;

    private volatile HikariDataSource dataSource;
    private volatile ExecutorService executor;

    public HikariDatabaseService(DatabaseSettings settings, Path dataFolder, PluginLogger logger) {
        this.settings = settings;
        this.dialect = SqlDialect.forType(settings.type());
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public void enable() {
        boolean isFileBased = settings.type() == DatabaseType.SQLITE;

        this.executor = Executors.newFixedThreadPool(
                isFileBased ? 1 : Math.max(2, settings.maxPoolSize()),
                new NamedThreadFactory("HalilIbrahimMMO-DB")
        );

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("HalilIbrahimMMO-Hikari");
        hikariConfig.setDriverClassName(driverClassNameFor(settings.type()));
        hikariConfig.setJdbcUrl(buildJdbcUrl());

        if (!isFileBased) {
            hikariConfig.setUsername(settings.username());
            hikariConfig.setPassword(settings.password());
        }

        hikariConfig.setMaximumPoolSize(isFileBased ? 1 : Math.max(1, settings.maxPoolSize()));
        hikariConfig.setMinimumIdle(isFileBased ? 1 : Math.max(0, settings.minimumIdle()));
        hikariConfig.setConnectionTimeout(settings.connectionTimeoutMs());
        hikariConfig.setIdleTimeout(settings.idleTimeoutMs());
        hikariConfig.setMaxLifetime(settings.maxLifetimeMs());

        this.dataSource = new HikariDataSource(hikariConfig);
        logger.info("Veritabani baglanti havuzu baslatildi: " + settings.type() + ".");
    }

    @Override
    public void disable() {
        ExecutorService currentExecutor = this.executor;
        if (currentExecutor != null) {
            currentExecutor.shutdown();
        }

        HikariDataSource currentDataSource = this.dataSource;
        if (currentDataSource != null) {
            currentDataSource.close();
        }

        logger.info("Veritabani baglanti havuzu kapatildi.");
    }

    @Override
    public DatabaseType activeType() {
        return settings.type();
    }

    @Override
    public SqlDialect dialect() {
        return dialect;
    }

    @Override
    public <T> CompletableFuture<T> execute(SqlFunction<T> action) {
        ExecutorService currentExecutor = this.executor;
        if (currentExecutor == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("DatabaseService is not enabled."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return action.apply(connection);
            } catch (SQLException exception) {
                throw new CompletionException(exception);
            }
        }, currentExecutor);
    }

    @Override
    public CompletableFuture<Void> executeVoid(SqlConsumer action) {
        return execute(connection -> {
            action.accept(connection);
            return null;
        });
    }

    private String buildJdbcUrl() {
        return switch (settings.type()) {
            case SQLITE -> "jdbc:sqlite:" + dataFolder.resolve(settings.sqliteFileName()).toAbsolutePath();
            case MYSQL -> "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.databaseName()
                    + "?useSSL=" + settings.useSsl() + "&characterEncoding=utf8&useUnicode=true";
            case MARIADB -> "jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/" + settings.databaseName()
                    + "?useSSL=" + settings.useSsl() + "&characterEncoding=utf8&useUnicode=true";
            case POSTGRESQL -> "jdbc:postgresql://" + settings.host() + ":" + settings.port() + "/" + settings.databaseName()
                    + "?ssl=" + settings.useSsl();
        };
    }

    private static String driverClassNameFor(DatabaseType type) {
        return switch (type) {
            case SQLITE -> "org.sqlite.JDBC";
            case MYSQL -> "com.mysql.cj.jdbc.Driver";
            case MARIADB -> "org.mariadb.jdbc.Driver";
            case POSTGRESQL -> "org.postgresql.Driver";
        };
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
