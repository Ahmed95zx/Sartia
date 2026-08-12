package il.ac.openu.sartia.service;

import il.ac.openu.sartia.dao.Database;
import il.ac.openu.sartia.model.CopyStatus;
import il.ac.openu.sartia.model.Rental;
import il.ac.openu.sartia.service.exception.ConflictException;
import il.ac.openu.sartia.service.exception.NotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the full lending lifecycle against a real database: renting takes a
 * disc off the shelf, returning puts it back, and the rules around who may do
 * what are enforced.
 *
 * <p>Complements {@code ConcurrentRentalIT}, which covers the same operations
 * under contention. Requires MySQL with {@code db/schema.sql} applied.
 */
@Tag("integration")
class RentalLifecycleIT {

    private static final long TEST_CATEGORY_ID = 1L;

    private final RentalService rentalService = new RentalService();
    private final CatalogService catalogService = new CatalogService();

    @BeforeAll
    static void initPool() {
        Database.init();
    }

    @AfterAll
    static void closePool() {
        Database.shutdown();
    }

    @Test
    @DisplayName("renting removes a copy from the shelf and returning restores it")
    void rentThenReturnRestoresStock() {
        long movieId = seedMovie("rental cycle", 2);
        long userId = seedUser("life");

        assertEquals(2, availableCopies(movieId), "both copies start on the shelf");

        Rental rental = rentalService.rent(userId, movieId);
        assertEquals(1, availableCopies(movieId), "renting takes one copy off the shelf");
        assertEquals(CopyStatus.RENTED, copyStatus(rental.getCopyId()));

        BigDecimal fee = rentalService.returnRental(rental.getId(), userId, false);

        assertEquals(0, BigDecimal.ZERO.compareTo(fee), "a rental returned on time owes nothing");
        assertEquals(2, availableCopies(movieId), "returning puts the copy back on the shelf");
        assertEquals(CopyStatus.AVAILABLE, copyStatus(rental.getCopyId()));
    }

    @Test
    @DisplayName("the rental stays in the customer's history after it is returned")
    void historySurvivesReturn() {
        long movieId = seedMovie("history", 1);
        long userId = seedUser("hist");

        Rental rental = rentalService.rent(userId, movieId);
        rentalService.returnRental(rental.getId(), userId, false);

        List<Rental> history = rentalService.historyFor(userId);
        assertEquals(1, history.size(), "the closed rental is still on record");
        assertFalse(history.get(0).isOpen());

        assertTrue(rentalService.openRentalsFor(userId).isEmpty(),
                "but it is no longer among the discs the customer holds");
    }

    @Test
    @DisplayName("a customer cannot return someone else's rental")
    void cannotReturnAnotherCustomersRental() {
        long movieId = seedMovie("foreign return", 1);
        long owner = seedUser("owner");
        long stranger = seedUser("stranger");

        Rental rental = rentalService.rent(owner, movieId);

        // Reported as "not found" rather than "forbidden" so the response does
        // not confirm that this rental id exists.
        assertThrows(NotFoundException.class,
                () -> rentalService.returnRental(rental.getId(), stranger, false));

        assertEquals(0, availableCopies(movieId), "the disc is still out");
    }

    @Test
    @DisplayName("an administrator may return any customer's rental")
    void adminMayReturnAnyRental() {
        long movieId = seedMovie("admin return", 1);
        long customer = seedUser("cust");
        long admin = seedUser("adm");

        Rental rental = rentalService.rent(customer, movieId);
        rentalService.returnRental(rental.getId(), admin, true);

        assertEquals(1, availableCopies(movieId));
    }

    @Test
    @DisplayName("returning the same rental twice is refused")
    void doubleReturnIsRefused() {
        long movieId = seedMovie("double return", 1);
        long userId = seedUser("dbl2");

        Rental rental = rentalService.rent(userId, movieId);
        rentalService.returnRental(rental.getId(), userId, false);

        assertThrows(ConflictException.class,
                () -> rentalService.returnRental(rental.getId(), userId, false));

        assertEquals(1, availableCopies(movieId), "the copy is not double-counted back in");
    }

    @Test
    @DisplayName("renting a title with no copies left is refused")
    void outOfStockIsRefused() {
        long movieId = seedMovie("out of stock", 1);
        long first = seedUser("first");
        long second = seedUser("second");

        rentalService.rent(first, movieId);

        ConflictException failure = assertThrows(ConflictException.class,
                () -> rentalService.rent(second, movieId));
        assertTrue(failure.getMessage().contains("currently out"),
                "the message should explain that every copy is out: " + failure.getMessage());
    }

    @Test
    @DisplayName("renting an unknown title is refused")
    void unknownMovieIsRefused() {
        long userId = seedUser("ghost");
        assertThrows(NotFoundException.class, () -> rentalService.rent(userId, 99_999_999L));
    }

    @Test
    @DisplayName("a copy marked lost never returns to the shelf")
    void lostCopyLeavesCirculation() {
        long movieId = seedMovie("lost copy", 1);
        long userId = seedUser("lost");

        Rental rental = rentalService.rent(userId, movieId);
        rentalService.markCopyLost(rental.getId());

        assertEquals(0, availableCopies(movieId));
        assertEquals(CopyStatus.LOST, copyStatus(rental.getCopyId()));
        assertTrue(rentalService.openRentalsFor(userId).isEmpty(),
                "the rental is closed even though the disc never came back");
    }

    /**
     * Regression test. Deleting a title whose copies carry rental history used
     * to pass the service's checks and then fail on the foreign key, surfacing
     * as an error page instead of an explanation.
     */
    @Test
    @DisplayName("a title that has ever been lent cannot be deleted")
    void cannotDeleteTitleWithRentalHistory() {
        long movieId = seedMovie("history blocks delete", 1);
        long userId = seedUser("hist2");

        Rental rental = rentalService.rent(userId, movieId);
        rentalService.returnRental(rental.getId(), userId, false);

        // Nothing is out on loan now, so only the history check can stop this.
        ConflictException failure = assertThrows(ConflictException.class,
                () -> catalogService.deleteMovie(movieId));
        assertTrue(failure.getMessage().contains("rental records"),
                "the message should explain that rental records exist: " + failure.getMessage());

        assertEquals(1, availableCopies(movieId), "the title is still in the catalogue");
    }

    @Test
    @DisplayName("a title that was never lent can be deleted")
    void unusedTitleCanBeDeleted() {
        long movieId = seedMovie("never rented", 2);

        catalogService.deleteMovie(movieId);

        assertEquals(0, availableCopies(movieId), "its copies were cascaded away");
    }

    /**
     * Regression test. The barcode sequence was derived from a row count, which
     * reissues a barcode already in use once any copy row is missing.
     */
    @Test
    @DisplayName("added copies never reuse a barcode after a gap in the sequence")
    void addedCopiesDoNotCollideAfterAGap() {
        long movieId = seedMovie("barcode sequence", 3);

        // Remove the middle copy, leaving sequence 1 and 3 in place.
        Database.runInTransaction(connection -> {
            try (var statement = connection.prepareStatement(
                    "DELETE FROM copies WHERE movie_id = ? AND barcode LIKE '%-2'")) {
                statement.setLong(1, movieId);
                statement.executeUpdate();
            }
        });

        // A count-based sequence would produce 3 here and collide with the
        // existing copy; the maximum-based one produces 4.
        catalogService.addCopies(movieId, 1);

        assertEquals(3, availableCopies(movieId), "the new copy was added alongside the survivors");
    }

    /* ------------------------------------------------------------------
     * Fixture
     * ------------------------------------------------------------------ */

    private long seedMovie(String title, int copies) {
        return Database.inTransaction(connection -> {
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
                    statement.setString(2, "LC-" + id + "-" + i);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            return id;
        });
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

    private int availableCopies(long movieId) {
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

    private CopyStatus copyStatus(long copyId) {
        return Database.readOnly(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT status FROM copies WHERE id = ?")) {
                statement.setLong(1, copyId);
                try (var rs = statement.executeQuery()) {
                    return rs.next() ? CopyStatus.valueOf(rs.getString(1)) : null;
                }
            }
        });
    }
}
