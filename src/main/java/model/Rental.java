package model;

import java.io.Serializable;
import util.AppConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A single checkout of a {@link Copy} by a {@link User}.
 *
 * <p>A rental is <em>open</em> while {@link #getReturnedAt()} is {@code null}.
 * The database enforces that a copy has at most one open rental at a time (see
 * the {@code active_flag} generated column in {@code schema.sql}); this class
 * only derives presentation state such as overdue status and late fees.
 */
public class Rental implements Serializable {

    /**
     * Version stamp used when Java turns an object of this class into bytes.
     *
     * <p>These objects are held in JSF view and session scope, and a servlet
     * container is allowed to serialise that state: to hand a session to
     * another server, or to keep it across a restart. Fixing the number by
     * hand means state written by an earlier build can still be read back
     * after a field is added, instead of failing on a version mismatch.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Charged per day started once a rental passes its due date.
     *
     * <p>Read from {@code sartia.properties}, like the lending period and the
     * borrowing limit: it is pricing policy, not logic, and changing it should
     * not require a rebuild.
     */
    public static final BigDecimal LATE_FEE_PER_DAY = AppConfig.lateFeePerDay();

    /** Primary key. Stays {@code 0} until the row has been inserted. */
    private long id;

    /**
     * The physical disc that was lent. Foreign key to {@code copies.id}.
     *
     * <p>Note that a loan points at a copy, not at a title. That is the
     * modelling decision the whole stock-control behaviour rests on.
     */
    private long copyId;

    /** The borrowing customer. Foreign key to {@code users.id}. */
    private long userId;

    /** Moment the disc left the shop. Set by the database default. */
    private LocalDateTime rentedAt;

    /**
     * Day the disc is due back: the rental date plus the lending period from
     * {@code sartia.properties}. A date rather than a timestamp, because the
     * shop counts overdue in whole days.
     */
    private LocalDate dueDate;

    /**
     * Moment the disc came back, or {@code null} while it is still out.
     *
     * <p>This single field is what "open" means. The database turns it into
     * the generated {@code active_flag} column, and a unique index on
     * {@code (copy_id, active_flag)} is what guarantees one open loan per disc.
     */
    private LocalDateTime returnedAt;

    /** Fee actually charged on return. {@code null} while the loan is open. */
    private BigDecimal lateFee;

    /* ------------------------------------------------------------------
     * Joined for display.
     *
     * None of these is a column on the rentals table. A loan on its own knows
     * only copy and user ids, but every screen that lists loans wants to show
     * the film and the borrower. The DAO joins them in once, which avoids
     * fetching each title and each customer separately for every row shown.
     * ------------------------------------------------------------------ */

    /** Film name, for the "my rentals" list and the returns screen. */
    private String movieTitle;

    /** Film id, so the list can link back to the film page. */
    private long movieId;

    /** Barcode of the lent disc, so staff can match it to the physical item. */
    private String barcode;

    /** Borrower's name, shown to the administrator on the returns screen. */
    private String userFullName;

    /** Borrower's login name, shown beside the full name to tell them apart. */
    private String username;

    /** Empty constructor required by the JavaBean convention. */
    public Rental() {
    }

    /** @return {@code true} while the disc is still with the customer. */
    public boolean isOpen() {
        return returnedAt == null;
    }

    /**
     * @return {@code true} if the rental is open and past its due date. A
     *         returned rental is never reported as overdue, even if it was
     *         returned late - that case is represented by {@link #getLateFee()}.
     */
    public boolean isOverdue() {
        return isOpen() && dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Days a currently-open rental is past due, or {@code 0} when it is not
     * overdue. Used by the UI to explain the fee the customer is accruing.
     */
    public long getDaysOverdue() {
        if (!isOverdue()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    /**
     * Fee that would be charged if the disc were returned now. For a rental
     * that has already been returned this is the fee that <em>was</em> charged.
     *
     * <p>Always scaled to two decimal places. {@link BigDecimal#equals} compares
     * scale as well as value, so an unscaled {@code 0} and a computed
     * {@code 0.00} are unequal objects despite being the same amount; pinning
     * the scale keeps the value comparable and keeps it displaying as currency.
     */
    public BigDecimal getProjectedLateFee() {
        BigDecimal fee;
        if (!isOpen()) {
            fee = lateFee == null ? BigDecimal.ZERO : lateFee;
        } else {
            fee = LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(getDaysOverdue()));
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /* ------------------------------------------------------------------
     * Accessors.
     *
     * JSF reads and writes these by name from the pages: an expression
     * such as #{movieBean.movie.title} calls getTitle(), and an input
     * bound to the same expression calls setTitle() when the form is
     * submitted. The DAO row mappers use them to fill an object from a
     * JDBC result set.
     *
     * They carry no logic of their own, so they are described here as a group
     * rather than repeating the same sentence above each one.
     * ------------------------------------------------------------------ */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCopyId() {
        return copyId;
    }

    public void setCopyId(long copyId) {
        this.copyId = copyId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public LocalDateTime getRentedAt() {
        return rentedAt;
    }

    public void setRentedAt(LocalDateTime rentedAt) {
        this.rentedAt = rentedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public long getMovieId() {
        return movieId;
    }

    public void setMovieId(long movieId) {
        this.movieId = movieId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Identity is the database id: two objects describe the same loan when they
     * carry the same id.
     *
     * <p>The {@code id != 0} test is deliberate. An object built in memory but
     * not yet inserted has no id, and without that test every unsaved object
     * would compare equal to every other unsaved object. An unsaved object is
     * therefore equal only to itself.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rental rental)) {
            return false;
        }
        return id != 0 && id == rental.id;
    }

    /**
     * Built from the same field {@link #equals(Object)} compares.
     *
     * <p>Java requires this: two objects that are equal must return the same
     * hash code, otherwise looking one up in a {@code HashMap} or
     * {@code HashSet} quietly fails to find it.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Short form for log messages and debugging. Never shown to a customer. */
    @Override
    public String toString() {
        return "Rental{id=" + id + ", copyId=" + copyId + ", open=" + isOpen() + '}';
    }
}
