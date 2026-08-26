package sartia.business.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A catalogue title.
 *
 * <p>A movie is not itself rentable - {@link Copy} is. The
 * {@link #getAvailableCopies()} and {@link #getTotalCopies()} fields are
 * derived by an aggregate in the catalogue query rather than stored, so they
 * cannot drift out of step with the {@code copies} table.
 */
public class Movie implements Serializable {

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

    /** The film's name. This is the field the catalogue search matches on. */
    private String title;

    /** Free-text synopsis shown on the film page. */
    private String description;

    /** Director's name, also matched by the catalogue search. */
    private String director;

    /**
     * Year of release. Boxed {@code Integer} rather than {@code int} so that
     * an unknown year can be held as {@code null} instead of a misleading 0.
     */
    private Integer releaseYear;

    /** Running time in minutes. Boxed for the same reason as the year. */
    private Integer durationMinutes;

    /** Genre this title is filed under. Foreign key to {@code categories.id}. */
    private long categoryId;

    /** Genre name, joined in by the query so the page need not look it up. */
    private String categoryName;

    /** Path of the cover image in the web application, e.g. /images/covers/1.jpg. */
    private String coverUrl;

    /**
     * Price charged per day of the loan.
     *
     * <p>{@code BigDecimal} rather than {@code double}: binary floating point
     * cannot hold a value such as 5.50 exactly, and money added up in
     * {@code double} drifts by fractions of an agora as the sums accumulate.
     */
    private BigDecimal dailyPrice = new BigDecimal("5.00");

    /** When the title was added to the catalogue. Set by the database default. */
    private LocalDateTime createdAt;

    /* ------------------------------------------------------------------
     * Derived values.
     *
     * None of these four is a column on the movies table. They are computed
     * by aggregates in the catalogue query and copied onto the object for the
     * page to display. Deriving them on every read, rather than keeping a
     * running total on the row, is what stops them drifting out of step with
     * the copies and reviews tables.
     * ------------------------------------------------------------------ */

    /** How many discs of this title the shop owns, in any state. */
    private int totalCopies;

    /** How many of those discs are on the shelf right now. */
    private int availableCopies;

    /** Mean of the review scores, or {@code null} when nobody has reviewed it. */
    private Double averageRating;

    /** How many reviews the average above was calculated from. */
    private int reviewCount;

    /** Empty constructor required by the JavaBean convention. */
    public Movie() {
    }

    /** @return {@code true} if at least one copy is on the shelf right now. */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /**
     * Rounded average rating for display, or {@code null} when the title has
     * not been reviewed yet.
     */
    public String getFormattedRating() {
        return averageRating == null ? null : String.format("%.1f", averageRating);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    /**
     * Identity is the database id: two objects describe the same title when they
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
        if (!(other instanceof Movie movie)) {
            return false;
        }
        return id != 0 && id == movie.id;
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
        return "Movie{id=" + id + ", title='" + title + "'}";
    }
}
