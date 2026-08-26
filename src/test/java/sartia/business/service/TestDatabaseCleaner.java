package sartia.business.service;

import sartia.persistence.dao.Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Removes every row the integration-test fixtures create.
 *
 * <p>The fixtures in {@code RentalLifecycleIT} and {@code ConcurrentRentalIT}
 * insert their own films, copies, customers and rentals so that no test depends
 * on the demo data. Historically they committed those rows and never deleted
 * them, so each run left permanent junk behind - films titled
 * {@code "admin return <nanoTime>"} or {@code "barcode sequence <nanoTime>"},
 * all filed under category&nbsp;1, plus throwaway {@code @test.local} users. When
 * the tests ran against the application's own {@code sartia} database that junk
 * surfaced directly in the catalogue.
 *
 * <p>Every fixture row carries a signature that real catalogue data never has,
 * which is what lets this run as a blunt, id-free sweep:
 * <ul>
 *   <li>test copies use barcodes {@code LC-...} (RentalLifecycleIT) or
 *       {@code IT-...} (ConcurrentRentalIT); real copies use {@code SRT-...};</li>
 *   <li>test users have {@code @test.local} e-mail addresses; real users do not.</li>
 * </ul>
 *
 * <p>Deletion runs in foreign-key-safe order. {@code rentals} reference
 * {@code copies} and {@code users} with no {@code ON DELETE} action, so those
 * rows must go first; {@code copies} and {@code reviews} then fall away with
 * their {@code movies} via {@code ON DELETE CASCADE}, and a final sweep clears
 * any stray copy whose film was already gone.
 */
final class TestDatabaseCleaner {

    private static final String TEST_COPY_BARCODES =
            "barcode LIKE 'LC-%' OR barcode LIKE 'IT-%'";
    private static final String TEST_USER_EMAILS =
            "email LIKE '%@test.local'";

    private TestDatabaseCleaner() {
        // Static helper - never instantiated.
    }

    /** Deletes all integration-test residue. Safe to call repeatedly. */
    static void cleanAll() {
        Database.inTransaction(connection -> {
            // 1. Rentals: FK to copies and users, no cascade, so clear first.
            exec(connection,
                 "DELETE r FROM rentals r JOIN copies c ON c.id = r.copy_id "
               + "WHERE c." + TEST_COPY_BARCODES);
            exec(connection,
                 "DELETE r FROM rentals r JOIN users u ON u.id = r.user_id "
               + "WHERE u." + TEST_USER_EMAILS);

            // 2. Reviews: cascade with their movie, but a test user may also have
            //    reviewed a real film; clear both cases explicitly.
            exec(connection,
                 "DELETE rv FROM reviews rv JOIN copies c ON c.movie_id = rv.movie_id "
               + "WHERE c." + TEST_COPY_BARCODES);
            exec(connection,
                 "DELETE rv FROM reviews rv JOIN users u ON u.id = rv.user_id "
               + "WHERE u." + TEST_USER_EMAILS);

            // 3. Movies that own a test copy - this cascades their copies and any
            //    remaining reviews away with them.
            exec(connection,
                 "DELETE m FROM movies m JOIN copies c ON c.movie_id = m.id "
               + "WHERE c." + TEST_COPY_BARCODES);

            // 4. Stray test copies whose movie was already deleted by a test.
            exec(connection,
                 "DELETE FROM copies WHERE " + TEST_COPY_BARCODES);

            // 5. The throwaway customers, now unreferenced.
            exec(connection,
                 "DELETE FROM users WHERE " + TEST_USER_EMAILS);
            return null;
        });
    }

    private static void exec(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
