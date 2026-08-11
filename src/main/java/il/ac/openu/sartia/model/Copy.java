package il.ac.openu.sartia.model;

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

    private static final long serialVersionUID = 1L;

    private long id;
    private long movieId;
    private String barcode;
    private CopyStatus status = CopyStatus.AVAILABLE;
    private LocalDateTime acquiredAt;

    public Copy() {
    }

    public Copy(long movieId, String barcode) {
        this.movieId = movieId;
        this.barcode = barcode;
    }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Copy{id=" + id + ", barcode='" + barcode + "', status=" + status + '}';
    }
}
