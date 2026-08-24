package model;

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

    private static final long serialVersionUID = 1L;

    private long id;
    private String title;
    private String description;
    private String director;
    private Integer releaseYear;
    private Integer durationMinutes;
    private long categoryId;
    private String categoryName;
    private String coverUrl;
    private BigDecimal dailyPrice = new BigDecimal("5.00");
    private LocalDateTime createdAt;

    /* --- derived, not stored --- */
    private int totalCopies;
    private int availableCopies;
    private Double averageRating;
    private int reviewCount;

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

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Movie{id=" + id + ", title='" + title + "'}";
    }
}
