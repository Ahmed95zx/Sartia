package il.ac.openu.sartia.model;

import java.io.Serializable;
import il.ac.openu.sartia.util.AppConfig;

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

    private static final long serialVersionUID = 1L;

    /**
     * Charged per day started once a rental passes its due date.
     *
     * <p>Read from {@code sartia.properties}, like the lending period and the
     * borrowing limit: it is pricing policy, not logic, and changing it should
     * not require a rebuild.
     */
    public static final BigDecimal LATE_FEE_PER_DAY = AppConfig.lateFeePerDay();

    private long id;
    private long copyId;
    private long userId;
    private LocalDateTime rentedAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;
    private BigDecimal lateFee;

    /* --- joined for display --- */
    private String movieTitle;
    private long movieId;
    private String barcode;
    private String userFullName;
    private String username;

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

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Rental{id=" + id + ", copyId=" + copyId + ", open=" + isOpen() + '}';
    }
}
