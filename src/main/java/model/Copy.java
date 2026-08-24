package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One physical disc of a {@link Movie} - the unit that is actually rented.
 *
 * <p>Modelling copies separately from titles is what allows the system to
 * satisfy the requirement that a title already out on loan cannot be lent
 * again, while still stocking several discs of a popular title.
 */
public class Copy implements Serializable {

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

    /** Primary key. Stays {@code 0} until the row has been inserted. */
    private long id;

    /** The title this disc is a copy of. Foreign key to {@code movies.id}. */
    private long movieId;

    /**
     * The sticker on the physical disc, unique across the whole shop. This is
     * what an administrator types on the returns screen to find the loan.
     */
    private String barcode;

    /** Shelf state. Only an AVAILABLE copy may be lent out. */
    private CopyStatus status = CopyStatus.AVAILABLE;

    /** When the shop bought this disc. Filled in by the database default. */
    private LocalDateTime acquiredAt;

    /**
     * Empty constructor required by the JavaBean convention: the DAO and JSF
     * both create the object first and fill it through the setters afterwards.
     */
    public Copy() {
    }

    /** Convenience constructor for a new disc that has not been saved yet. */
    public Copy(long movieId, String barcode) {
        this.movieId = movieId;
        this.barcode = barcode;
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

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    public LocalDateTime getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(LocalDateTime acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    /**
     * Identity is the database id: two objects describe the same disc when they
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
        if (!(other instanceof Copy copy)) {
            return false;
        }
        return id != 0 && id == copy.id;
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
        return "Copy{id=" + id + ", barcode='" + barcode + "', status=" + status + '}';
    }
}
