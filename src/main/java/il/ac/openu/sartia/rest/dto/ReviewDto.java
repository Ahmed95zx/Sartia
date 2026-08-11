package il.ac.openu.sartia.rest.dto;

import il.ac.openu.sartia.model.Review;

import java.time.LocalDateTime;

/**
 * The public JSON shape of a review.
 *
 * <p>Carries the reviewer's display name but never their id or contact details:
 * a public endpoint should not become a way to enumerate the customer base.
 *
 * @param rating    score from 1 to 5
 * @param comment   free text, may be null
 * @param reviewer  display name of the customer who wrote it
 * @param createdAt when it was written
 */
public record ReviewDto(int rating, String comment, String reviewer, LocalDateTime createdAt) {

    public static ReviewDto from(Review review) {
        return new ReviewDto(
                review.getRating(),
                review.getComment(),
                review.getUserFullName(),
                review.getCreatedAt());
    }
}
