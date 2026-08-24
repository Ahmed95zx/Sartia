package service;

import dao.Database;
import service.exception.ConflictException;
import util.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies, against a real MySQL instance, that concurrent customers cannot
 * over-lend the same stock.
 *
 * <p>This is the requirement the specification states as "the system must not
 * lend a film that has not yet been returned", and it is the one claim in the
 * project that cannot be demonstrated by reading the code - a race either
 * happens or it does not. The tests therefore drive
 * {@link RentalService#rent(long, long)} from many threads at once and assert
 * on what the database contains afterwards.
 *
 * <p>Requires MySQL running with {@code db/schema.sql} applied. Run with:
 * <pre>mvn test -Dtest=ConcurrentRentalIT</pre>
 * Each test seeds its own isolated fixture and cleans up after itself.
 */
@Tag("integration")
class ConcurrentRentalIT {

    private static final long TEST_CATEGORY_ID = 1L;

    private final RentalService rentalService = new RentalService();

    private long movieId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeAll
    static void initPool() {
        Database.init();
    }

    @AfterAll
    static void closePool() {
        Database.shutdown();
    }

    @BeforeEach
    void reset() {
        userIds.clear();
    }

    /**
     * The central test: more customers than copies, all pressing "rent" at the
     * same instant.
     */
    @Test
    @DisplayName("20 customers racing for 3 copies produce exactly 3 rentals")
    void doesNotOverLend() throws Exception {
        int copies = 3;
        int customers = 20;

        seedMovie("load test", copies);
        seedUsers(customers);

        RaceOutcome outcome = raceToRent(customers);

        assertEquals(copies, outcome.successes(),
                "exactly one rental per copy should have been created");
        assertEquals(customers - copies, outcome.conflicts(),
                "every other customer should have been told the film was taken");
        assertEquals(0, outcome.unexpectedFailures(),
                "no customer should see an error other than a clean conflict: " + outcome.errors());

        assertEquals(copies, countOpenRentalsForMovie(),
                "the database should hold exactly one open rental per copy");
        assertEquals(0, countAvailableCopies(),
                "every copy should now be marked RENTED");
    }

    /** The tightest case: a single copy, many customers. */
    @Test
    @DisplayName("15 customers racing for the last copy produce exactly 1 rental")
    void lastCopyGoesToExactlyOneCustomer() throws Exception {
        seedMovie("last copy", 1);
        seedUsers(15);

        RaceOutcome outcome = raceToRent(15);

        assertEquals(1, outcome.successes());
        assertEquals(14, outcome.conflicts());
        assertEquals(0, outcome.unexpectedFailures(), outcome.errors().toString());
        assertEquals(1, countOpenRentalsForMovie());
    }

    /**
     * Ample stock: contention should not cause spurious failures either.
     * A lock strategy that serialised or timed out would show up here.
     */
    @Test
    @DisplayName("with a copy for everyone, every customer succeeds")
    void everyoneSucceedsWhenStockIsSufficient() throws Exception {
        int customers = 10;
        seedMovie("enough stock", customers);
        seedUsers(customers);

        RaceOutcome outcome = raceToRent(customers);

        assertEquals(customers, outcome.successes(), outcome.errors().toString());
        assertEquals(0, outcome.conflicts());
        assertEquals(customers, countOpenRentalsForMovie());
    }

    /** The same customer double-clicking must not consume two copies. */
    @Test
    @DisplayName("one customer submitting twice at once gets a single rental")
    void doubleSubmitByOneCustomerRentsOnce() throws Exception {
        seedMovie("double click", 5);
        long userId = seedUser("dbl");

        int attempts = 8;
        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(pool.submit(attemptRent(userId, startLine)));
            }
            startLine.countDown();

            RaceOutcome outcome = collect(futures);
            assertEquals(1, outcome.successes(),
                    "the duplicate-title rule should stop the second submission");
            assertEquals(attempts - 1, outcome.conflicts());
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(10, TimeUnit.SECONDS);
        }

        assertEquals(1, countOpenRentalsForMovie());
    }

    /**
     * The borrowing limit is also a per-customer count, so it is vulnerable to
     * the same read-then-act race as the duplicate-title rule: several requests
     * arriving together would each read the old count and each believe there
     * was room.
     */
    @Test
    @DisplayName("simultaneous requests cannot push one customer past the borrowing limit")
    void borrowingLimitHoldsUnderConcurrency() throws Exception {
        int limit = AppConfig.maxConcurrentRentalsPerUser();
        int attempts = limit + 5;

        long userId = seedUser("limit");

        // A separate title per attempt, so the duplicate-title rule is not what
        // stops them - the limit itself has to.
        List<Long> movieIds = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            seedMovie("quota " + i, 1);
            movieIds.add(movieId);
        }

        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (long id : movieIds) {
                futures.add(pool.submit(() -> {
                    startLine.await();
                    try {
                        rentalService.rent(userId, id);
                        return "OK";
                    } catch (ConflictException expected) {
                        return "CONFLICT";
                    } catch (RuntimeException unexpected) {
                        return "ERROR: " + unexpected;
                    }
                }));
            }
            startLine.countDown();

            RaceOutcome outcome = collect(futures);
            assertEquals(limit, outcome.successes(),
                    "the customer should end up holding exactly the maximum, never more");
            assertEquals(attempts - limit, outcome.conflicts());
            assertEquals(0, outcome.unexpectedFailures(), outcome.errors().toString());
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }

        assertEquals(limit, countOpenRentalsForUser(userId));
    }

    /* ------------------------------------------------------------------
     * Harness
     * ------------------------------------------------------------------ */

    /** Aggregated result of one race. */
    private record RaceOutcome(int successes, int conflicts, int unexpectedFailures, List<String> errors) {
    }

    /**
     * Starts one thread per customer, holds them all on a latch, then releases
     * them together so the calls overlap as tightly as the machine allows.
     */
    private RaceOutcome raceToRent(int customers) throws Exception {
        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(customers);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (long userId : userIds) {
                futures.add(pool.submit(attemptRent(userId, startLine)));
            }
            startLine.countDown();
            return collect(futures);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    private Callable<String> attemptRent(long userId, CountDownLatch startLine) {
        return () -> {
            startLine.await();
            try {
                rentalService.rent(userId, movieId);
                return "OK";
            } catch (ConflictException expected) {
                return "CONFLICT";
            } catch (RuntimeException unexpected) {
                return "ERROR: " + unexpected.getClass().getSimpleName() + " " + unexpected.getMessage();
            }
        };
    }

    private RaceOutcome collect(List<Future<String>> futures) throws Exception {
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<String> errors = new ArrayList<>();

        for (Future<String> future : futures) {
            String result = future.get(30, TimeUnit.SECONDS);
            if ("OK".equals(result)) {
                ok.incrementAndGet();
            } else if ("CONFLICT".equals(result)) {
                conflict.incrementAndGet();
            } else {
                errors.add(result);
            }
        }
        return new RaceOutcome(ok.get(), conflict.get(), errors.size(), errors);
    }

    /* ------------------------------------------------------------------
     * Fixture
     * ------------------------------------------------------------------ */

    private void seedMovie(String title, int copies) {
        movieId = Database.inTransaction(connection -> {
            long id;
            try (var statement = connection.prepareStatement(
                    "INSERT INTO movies (title, category_id, daily_price) VALUES (?, ?, 5.00)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, title + " " + System.nanoTime());
                statement.setLong(2, TEST_CATEGORY_ID);
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }

            try (var statement = connection.prepareStatement(
                    "INSERT INTO copies (movie_id, barcode, status) VALUES (?, ?, 'AVAILABLE')")) {
                for (int i = 1; i <= copies; i++) {
                    statement.setLong(1, id);
                    statement.setString(2, "IT-" + id + "-" + i);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return id;
        });
    }

    private void seedUsers(int count) {
        for (int i = 0; i < count; i++) {
            userIds.add(seedUser("it" + i));
        }
    }

    private long seedUser(String prefix) {
        String unique = prefix + "_" + System.nanoTime();
        return Database.inTransaction(connection -> {
            try (var statement = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash, email, full_name, role) "
                  + "VALUES (?, 'x', ?, ?, 'CUSTOMER')",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, unique);
                statement.setString(2, unique + "@test.local");
                statement.setString(3, "test " + unique);
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    keys.next();
                    return keys.getLong(1);
                }
            }
        });
    }

    private int countOpenRentalsForMovie() {
        return Database.readOnly(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT COUNT(*)
                    FROM   rentals r
                    JOIN   copies cp ON cp.id = r.copy_id
                    WHERE  cp.movie_id = ? AND r.returned_at IS NULL
                    """)) {
                statement.setLong(1, movieId);
                try (var rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    private int countOpenRentalsForUser(long userId) {
        return Database.readOnly(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM rentals WHERE user_id = ? AND returned_at IS NULL")) {
                statement.setLong(1, userId);
                try (var rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    private int countAvailableCopies() {
        return Database.readOnly(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM copies WHERE movie_id = ? AND status = 'AVAILABLE'")) {
                statement.setLong(1, movieId);
                try (var rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

}
