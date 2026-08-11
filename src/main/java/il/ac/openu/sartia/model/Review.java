package il.ac.openu.sartia.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A customer's rating and free-text opinion of a {@link Movie}.
 *
 * <p>The schema carries a unique constraint on {@code (movie_id, user_id)}, so
 * a customer revising their opinion updates the existing row rather than
 * stacking duplicates.
 */
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long movieId;
    private long userId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    /* --- joined for display --- */
    private String userFullName;

    public Review() {
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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Review review)) {
            return false;
        }
        return id != 0 && id == review.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Review{id=" + id + ", movieId=" + movieId + ", rating=" + rating + '}';
    }
}
