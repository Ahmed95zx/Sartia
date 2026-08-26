package sartia.business.service;

import sartia.persistence.dao.CopyDao;
import sartia.persistence.dao.Database;
import sartia.persistence.dao.MovieDao;
import sartia.persistence.dao.RentalDao;
import sartia.persistence.dao.UserDao;
import sartia.business.domain.CopyStatus;
import sartia.business.domain.Movie;
import sartia.business.domain.Rental;
import sartia.business.exception.ConflictException;
import sartia.business.exception.NotFoundException;
import sartia.common.config.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalLong;
import java.util.logging.Logger;

/**
 * Lending and returning discs.
 *
 * <p>This is the part of the system that several customers can hit at the same
 * instant for the same title, so the correctness argument is worth stating
 * plainly. Renting runs as one transaction that:
 * <ol>
 *   <li>locks the borrowing customer's own row, serialising that customer's
 *       concurrent requests;</li>
 *   <li>checks the per-customer rules - borrowing limit, and not already
 *       holding this title - which the lock above has just made reliable;</li>
 *   <li>takes an exclusive row lock on exactly one available copy
 *       ({@code SELECT ... FOR UPDATE SKIP LOCKED});</li>
 *   <li>flips that copy to RENTED only if it is still AVAILABLE;</li>
 *   <li>inserts the rental row, which the unique index
 *       {@code uq_rentals_copy_active} independently guarantees is the only
 *       open rental for that copy.</li>
 * </ol>
 * Every step commits together or not at all.
 *
 * <p>There are two distinct races here, and they need different defences:
 * <ul>
 *   <li><b>Different customers, same title.</b> Guarded by the copy lock and,
 *       behind it, the unique index. Twenty customers racing for three discs
 *       produce exactly three rentals; the rest get a clear "already taken"
 *       message.</li>
 *   <li><b>One customer, several simultaneous requests</b> - a double-clicked
 *       button or two open tabs. The copy lock does not help, because each
 *       request would take a <em>different</em> disc. This is what the user
 *       lock in step 1 is for. It was added after an integration test caught
 *       exactly this case lending the same customer two copies of one film.</li>
 * </ul>
 * Both are covered by {@code ConcurrentRentalIT}.
 *
 * <p>The DAOs are stateless and hold no connection of their own, so a single
 * instance of each is shared across all requests.
 */
@ApplicationScoped
public class RentalService {

    private static final Logger LOG = Logger.getLogger(RentalService.class.getName());

    private final RentalDao rentalDao = new RentalDao();
    private final CopyDao copyDao = new CopyDao();
    private final MovieDao movieDao = new MovieDao();
    private final UserDao userDao = new UserDao();

    /**
     * Lends one copy of a title to a customer.
     *
     * @param userId  the borrowing customer
     * @param movieId the title requested
     * @return the rental that was opened
     * @throws NotFoundException if the title does not exist
     * @throws ConflictException if the customer is at their limit, already
     *                           holds this title, or every copy was taken
     */
    public Rental rent(long userId, long movieId) {
        int maxConcurrent = AppConfig.maxConcurrentRentalsPerUser();
        LocalDate dueDate = LocalDate.now().plusDays(AppConfig.rentalPeriodDays());

        return Database.inTransaction(connection -> {
            // Lock this customer's row before reading anything else. The checks
            // below are counted per customer, and without the lock both are
            // read-then-act: a double-submitted form, or two open tabs, would
            // have both requests read the same old state and both conclude they
            // were within the rules.
            //
            // The lock alone is not enough. Under MySQL's default REPEATABLE
            // READ, a transaction's snapshot is fixed at its first read and
            // every later non-locking read is served from it - so the checks
            // below would answer from a view of the data taken before the lock
            // was granted. These transactions therefore run at READ COMMITTED
            // (see Database), where each statement sees the latest committed
            // state. Taking the lock first is kept as the clearer ordering:
            // acquire, then read what the lock protects.
            //
            // Lock order matters too: user first, then copy. Every transaction
            // that takes both does so in this order, which is what stops two of
            // them deadlocking by grabbing the pair in opposite orders.
            if (!userDao.lockUser(connection, userId)) {
                throw new NotFoundException("User not found");
            }

            Movie movie = movieDao.findById(connection, movieId)
                    .orElseThrow(() -> NotFoundException.movie(movieId));

            // --- per-customer policy checks ---

            int alreadyOut = rentalDao.countOpenByUser(connection, userId);
            if (alreadyOut >= maxConcurrent) {
                throw new ConflictException(
                        "You may not rent more than " + maxConcurrent + " films at a time. "
                        + "Please return one before renting another.");
            }

            if (rentalDao.hasOpenRentalOfMovie(connection, userId, movieId)) {
                throw new ConflictException("You already have \"" + movie.getTitle() + "\" on loan.");
            }

            // --- reserve a physical copy ---

            OptionalLong lockedCopy = copyDao.lockAvailableCopy(connection, movieId);
            if (lockedCopy.isEmpty()) {
                throw new ConflictException(
                        "All copies of \"" + movie.getTitle() + "\" are currently out. "
                        + "Please try again later.");
            }
            long copyId = lockedCopy.getAsLong();

            // The row is locked, so this cannot be lost to a competing update.
            // It is still written conditionally: if the guarantee is ever weakened,
            // this fails loudly instead of silently double-lending a disc.
            if (!copyDao.updateStatus(connection, copyId, CopyStatus.AVAILABLE, CopyStatus.RENTED)) {
                throw new ConflictException("That copy was taken by another customer. Please try again.");
            }

            try {
                Rental rental = rentalDao.insert(connection, copyId, userId, dueDate);
                rental.setMovieId(movieId);
                rental.setMovieTitle(movie.getTitle());
                LOG.info(() -> "Rented copy " + copyId + " of movie " + movieId + " to user " + userId);
                return rental;
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                // uq_rentals_copy_active fired: the copy already had an open
                // rental. Unreachable while the lock above is in place - kept so
                // the invariant is enforced rather than merely assumed.
                throw new ConflictException("That copy is already on loan. Please try again.");
            }
        });
    }

    /**
     * Takes a disc back, closing its rental and returning it to the shelf.
     *
     * <p>Both writes happen in one transaction: a crash between them would
     * otherwise leave a disc that is marked available but still attached to an
     * open rental, or vice versa.
     *
     * @param rentalId    the rental to close
     * @param requestedBy the user asking; a customer may only return their own
     *                    disc, an administrator may return anyone's
     * @param isAdmin     whether {@code requestedBy} holds the ADMIN role
     * @return the late fee charged, zero when returned on time
     */
    public BigDecimal returnRental(long rentalId, long requestedBy, boolean isAdmin) {
        return Database.inTransaction(connection -> {
            Rental rental = rentalDao.findById(connection, rentalId)
                    .orElseThrow(() -> NotFoundException.rental(rentalId));

            if (!isAdmin && rental.getUserId() != requestedBy) {
                // Reported as "not found" rather than "forbidden" so the response
                // does not confirm that someone else's rental id exists.
                throw NotFoundException.rental(rentalId);
            }

            if (!rental.isOpen()) {
                throw new ConflictException("This rental was already returned on " + rental.getReturnedAt().toLocalDate());
            }

            BigDecimal lateFee = rental.getProjectedLateFee();

            if (!rentalDao.close(connection, rentalId, lateFee)) {
                // Another request closed it between our read and our write.
                throw new ConflictException("This rental was already returned by another request.");
            }

            // Checked, not assumed: if the copy is not in the state this rental
            // implies, the two tables disagree and committing would leave a disc
            // that is neither on the shelf nor accounted for. Rolling back keeps
            // them consistent and makes the fault visible.
            if (!copyDao.updateStatus(connection, rental.getCopyId(),
                                      CopyStatus.RENTED, CopyStatus.AVAILABLE)) {
                throw new ConflictException(
                        "The copy's state does not match the rental. Please refresh and try again.");
            }

            LOG.info(() -> "Returned rental " + rentalId + " (late fee " + lateFee + ")");
            return lateFee;
        });
    }

    /** Marks a copy written off. Only reachable from the administration screens. */
    public void markCopyLost(long rentalId) {
        Database.runInTransaction(connection -> {
            Rental rental = rentalDao.findById(connection, rentalId)
                    .orElseThrow(() -> NotFoundException.rental(rentalId));

            if (!rental.isOpen()) {
                throw new ConflictException("A rental that has already been returned cannot be marked as lost.");
            }

            rentalDao.close(connection, rentalId, rental.getProjectedLateFee());

            if (!copyDao.updateStatus(connection, rental.getCopyId(),
                                      CopyStatus.RENTED, CopyStatus.LOST)) {
                throw new ConflictException(
                        "The copy's state does not match the rental. Please refresh and try again.");
            }
        });
    }

    public List<Rental> historyFor(long userId) {
        return Database.readOnly(connection -> rentalDao.findByUser(connection, userId));
    }

    public List<Rental> openRentalsFor(long userId) {
        return Database.readOnly(connection -> rentalDao.findOpenByUser(connection, userId));
    }

    public List<Rental> allOpenRentals() {
        return Database.readOnly(rentalDao::findAllOpen);
    }

    public List<Rental> overdueRentals() {
        return Database.readOnly(rentalDao::findOverdue);
    }

    /**
     * Whether the UI should offer a "rent" button - the customer is under their
     * limit, does not already hold the title, and a copy is on the shelf.
     *
     * <p>This is advisory only. It reads without locking, so the answer can be
     * stale by the time the customer clicks; {@link #rent} re-checks everything
     * under lock and is the authority.
     */
    public boolean canRent(long userId, long movieId) {
        return Database.readOnly(connection -> {
            if (rentalDao.countOpenByUser(connection, userId) >= AppConfig.maxConcurrentRentalsPerUser()) {
                return false;
            }
            if (rentalDao.hasOpenRentalOfMovie(connection, userId, movieId)) {
                return false;
            }
            return copyDao.countAvailable(connection, movieId) > 0;
        });
    }
}
