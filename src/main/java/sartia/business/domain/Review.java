package sartia.business.domain;

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

    /** The title being reviewed. Foreign key to {@code movies.id}. */
    private long movieId;

    /** Who wrote it. Foreign key to {@code users.id}. */
    private long userId;

    /** Score from 1 to 5. The range is checked in the service and in the schema. */
    private int rating;

    /** The written opinion. Optional: a customer may rate without writing. */
    private String comment;

    /** When the review was written. Set by the database default. */
    private LocalDateTime createdAt;

    /**
     * Author's name, joined in by the query rather than stored on the row.
     *
     * <p>The film page lists reviews with their authors, and carrying the name
     * on the object avoids looking each one up separately, which would cost
     * one extra query per review shown.
     */
    private String userFullName;

    /** Empty constructor required by the JavaBean convention. */
    public Review() {
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

    /**
     * Identity is the database id: two objects describe the same review when they
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
        if (!(other instanceof Review review)) {
            return false;
        }
        return id != 0 && id == review.id;
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
        return "Review{id=" + id + ", movieId=" + movieId + ", rating=" + rating + '}';
    }
}
