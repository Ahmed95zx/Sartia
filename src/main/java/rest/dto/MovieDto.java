package rest.dto;

import model.Movie;

import java.math.BigDecimal;

/**
 * The public JSON shape of a catalogue title.
 *
 * <p>The API returns this rather than the {@link Movie} entity so that the
 * wire format is a deliberate decision instead of a side effect of the
 * database schema. Renaming a column then cannot silently break a client, and
 * fields that exist for internal bookkeeping never leak.
 *
 * @param id             catalogue identifier
 * @param title          film title
 * @param description    synopsis, may be null
 * @param director       director, may be null
 * @param releaseYear    year of release, may be null
 * @param durationMinutes running time, may be null
 * @param category       category name
 * @param dailyPrice     price per day in shekels
 * @param totalCopies    discs owned
 * @param availableCopies discs currently on the shelf
 * @param averageRating  mean review score, null when unreviewed
 * @param reviewCount    number of reviews
 */
public record MovieDto(
        long id,
        String title,
        String description,
        String director,
        Integer releaseYear,
        Integer durationMinutes,
        String category,
        BigDecimal dailyPrice,
        int totalCopies,
        int availableCopies,
        Double averageRating,
        int reviewCount) {

    public static MovieDto from(Movie movie) {
        return new MovieDto(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDirector(),
                movie.getReleaseYear(),
                movie.getDurationMinutes(),
                movie.getCategoryName(),
                movie.getDailyPrice(),
                movie.getTotalCopies(),
                movie.getAvailableCopies(),
                movie.getAverageRating(),
                movie.getReviewCount());
    }
}
