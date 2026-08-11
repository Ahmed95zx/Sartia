package il.ac.openu.sartia.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the JDBC connection pool and defines how work is run against it.
 *
 * <h2>Why a pool</h2>
 * Opening a TCP connection and authenticating costs several milliseconds. Doing
 * that per HTTP request collapses under concurrent load and exhausts the
 * server's connection limit, so connections are borrowed from a fixed-size pool
 * and returned immediately after use.
 *
 * <h2>Transaction boundaries</h2>
 * Every unit of work goes through {@link #inTransaction} or {@link #readOnly}.
 * Both hand a {@link Connection} to the caller and guarantee it is committed or
 * rolled back and then closed exactly once, no matter how the caller exits.
 * DAOs therefore accept a {@code Connection} as their first argument instead of
 * acquiring their own - that is what lets a service compose several DAO calls
 * into a single atomic transaction.
 *
 * <h2>Deadlock retry</h2>
 * Row-level locking means two transactions touching the same rows in different
 * orders can deadlock; InnoDB resolves this by killing one of them. That is a
 * transient, expected condition rather than a bug, so {@link #inTransaction}
 * retries such transactions a bounded number of times before giving up.
 */
public final class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    /** Attempts for a transaction that keeps losing deadlock arbitration. */
    private static final int MAX_DEADLOCK_RETRIES = 3;

    /** MySQL: ER_LOCK_DEADLOCK. */
    private static final int ERR_DEADLOCK = 1213;

    /** MySQL: ER_LOCK_WAIT_TIMEOUT. */
    private static final int ERR_LOCK_WAIT_TIMEOUT = 1205;

    /**
     * Isolation used unless a caller asks for another.
     *
     * <p>READ COMMITTED rather than MySQL's REPEATABLE READ default: see
     * {@link #inTransaction(int, TxWork)} for why the difference matters to
     * any transaction that locks a row and then reads.
     */
    private static final int DEFAULT_ISOLATION = Connection.TRANSACTION_READ_COMMITTED;

    private static volatile HikariDataSource dataSource;

    private Database() {
        // Static holder - never instantiated.
    }

    /**
     * Work to perform with a borrowed connection.
     *
     * @param <T> the value produced by the unit of work
     */
    @FunctionalInterface
    public interface TxWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    /** Work that produces no value. */
    @FunctionalInterface
    public interface VoidTxWork {
        void apply(Connection connection) throws SQLException;
    }

    /**
     * Builds the pool from {@code sartia.properties} on the classpath. Any
     * property may be overridden by a JVM system property of the same name,
     * which is how the deployment is pointed at a different database without
     * rebuilding the WAR.
     */
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }

        Properties props = loadProperties();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolve(props, "db.url"));
        config.setUsername(resolve(props, "db.user"));
        config.setPassword(resolve(props, "db.password"));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(Integer.parseInt(resolve(props, "db.pool.maxSize")));
        config.setMinimumIdle(Integer.parseInt(resolve(props, "db.pool.minIdle")));
        config.setConnectionTimeout(Long.parseLong(resolve(props, "db.pool.connectionTimeoutMs")));
        config.setPoolName("sartia-pool");

        // Fail fast on a misconfigured database rather than surfacing the
        // problem later as a confusing timeout on the first user request.
        config.setInitializationFailTimeout(10_000);

        dataSource = new HikariDataSource(config);
        LOG.info(() -> "Connection pool initialised against " + config.getJdbcUrl());
    }

    /** Closes the pool. Called from the servlet context listener on undeploy. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            LOG.info("Connection pool closed");
        }
    }

    /**
     * Runs {@code work} inside a single transaction, committing on normal
     * return and rolling back on any exception.
     *
     * <p>Retries automatically when InnoDB reports a deadlock or a lock wait
     * timeout, since both mean "your transaction lost a race, try again" rather
     * than "your query is wrong".
     *
     * @throws DataAccessException if the work fails for a non-transient reason,
     *                             or keeps deadlocking after {@value #MAX_DEADLOCK_RETRIES} attempts
     */
    public static <T> T inTransaction(TxWork<T> work) {
        return inTransaction(DEFAULT_ISOLATION, work);
    }

    /**
     * {@link #inTransaction(TxWork)} at an explicit isolation level.
     *
     * <p>Needed because MySQL's default, REPEATABLE READ, fixes a consistent
     * snapshot at the transaction's first read and serves every later
     * non-locking read from it. A transaction that takes a row lock in order to
     * serialise itself against others would then still read <em>pre-lock</em>
     * data afterwards, and act on it - which defeats the purpose of the lock.
     * At READ COMMITTED each statement sees the latest committed state, so the
     * checks made while holding a lock reflect reality.
     *
     * @param isolationLevel one of the {@link Connection} {@code TRANSACTION_*} constants
     */
    public static <T> T inTransaction(int isolationLevel, TxWork<T> work) {
        for (int attempt = 1; attempt <= MAX_DEADLOCK_RETRIES; attempt++) {
            try (Connection connection = borrow()) {
                connection.setAutoCommit(false);
                connection.setTransactionIsolation(isolationLevel);
                try {
                    T result = work.apply(connection);
                    connection.commit();
                    return result;
                } catch (SQLException | RuntimeException failure) {
                    rollbackQuietly(connection);
                    throw failure;
                }
            } catch (SQLException sqlFailure) {
                // The final attempt throws here rather than falling out of the
                // loop, so the caller always receives the actual database error
                // rather than a summary of it.
                if (!isTransient(sqlFailure) || attempt == MAX_DEADLOCK_RETRIES) {
                    throw new DataAccessException("Transaction failed: " + sqlFailure.getMessage(), sqlFailure);
                }
                LOG.log(Level.WARNING,
                        "Transient lock conflict (attempt {0}/{1}), retrying",
                        new Object[] { attempt, MAX_DEADLOCK_RETRIES });
                backOff(attempt);
            }
        }

        // Unreachable: every iteration either returns or throws.
        throw new IllegalStateException("retry loop exited without a result");
    }

    /**
     * {@link #inTransaction} for work that produces no value.
     *
     * <p>Deliberately a different name rather than an overload: a lambda whose
     * body is a call to a void method is compatible with both shapes, so an
     * overloaded pair would be ambiguous at exactly the call sites that need it.
     */
    public static void runInTransaction(VoidTxWork work) {
        inTransaction(connection -> {
            work.apply(connection);
            return null;
        });
    }

    /**
     * Runs read-only work. The connection stays in auto-commit mode, so the
     * driver does not open an explicit transaction for a single SELECT.
     */
    public static <T> T readOnly(TxWork<T> work) {
        try (Connection connection = borrow()) {
            connection.setReadOnly(true);
            return work.apply(connection);
        } catch (SQLException failure) {
            throw new DataAccessException("Query failed: " + failure.getMessage(), failure);
        }
    }

    private static Connection borrow() throws SQLException {
        if (dataSource == null) {
            // Covers unit tests and any code path that runs outside the container.
            init();
        }
        return dataSource.getConnection();
    }

    /**
     * @return {@code true} for errors that mean "retry may succeed" - InnoDB
     *         deadlock victims and lock wait timeouts.
     */
    private static boolean isTransient(SQLException failure) {
        if (failure instanceof SQLTransactionRollbackException) {
            return true;
        }
        int code = failure.getErrorCode();
        return code == ERR_DEADLOCK || code == ERR_LOCK_WAIT_TIMEOUT;
    }

    /** Short escalating pause so retries do not collide again immediately. */
    private static void backOff(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("Interrupted while retrying transaction", interrupted);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            LOG.log(Level.SEVERE, "Rollback failed", rollbackFailure);
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("sartia.properties")) {
            if (in == null) {
                throw new DataAccessException("sartia.properties not found on the classpath");
            }
            props.load(in);
        } catch (IOException failure) {
            throw new DataAccessException("Could not read sartia.properties", failure);
        }
        return props;
    }

    /** System property wins over the packaged default, so deployments can override. */
    private static String resolve(Properties props, String key) {
        String value = System.getProperty(key, props.getProperty(key));
        if (value == null) {
            throw new DataAccessException("Missing configuration key: " + key);
        }
        return value.trim();
    }
}
