package service;

import dao.Database;
import dao.RentalDao;
import dao.ReviewDao;
import model.Review;
import service.exception.ConflictException;
import service.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/**
 * Customer ratings and comments on titles.
 *
 * <p>A business-layer class: it holds the rules about reviewing and calls the
 * DAOs to carry them out. It is reached from both the film page and the REST
 * resources, which is exactly why the rules live here and not in either of
 * them.
 *
 * <p>{@code @ApplicationScoped} means CDI keeps one instance for the whole
 * application, shared by every request. That is safe here only because the
 * object holds no per-user state: the two fields below are stateless helpers,
 * and everything that varies between callers arrives as a parameter. A field
 * remembering "the current customer" would be a bug the moment two people
 * used the site at once.
 */
@ApplicationScoped
public class ReviewService {

    /** Longest review accepted, matching the column width in the schema. */
    private static final int MAX_COMMENT_LENGTH = 2000;

    /** Data access for the reviews table. */
    private final ReviewDao reviewDao = new ReviewDao();

    /** Data access for rentals, used to check the customer actually rented the film. */
    private final RentalDao rentalDao = new RentalDao();

    /**
     * Records or replaces a customer's review of a title.
     *
     * <p>Only customers who have actually rented the film may review it. Without
     * that rule the ratings shown on the catalogue would say nothing about the
     * films and everything about who felt like typing.
     *
     * @throws ValidationException if the rating is outside 1-5 or the comment is too long
     * @throws ConflictException   if the customer never rented the title
     */
    public void submit(long movieId, long userId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("The rating must be between 1 and 5");
        }
        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("The review is too long (up to " + MAX_COMMENT_LENGTH + " characters)");
        }

        Database.runInTransaction(connection -> {
            if (!rentalDao.hasEverRentedMovie(connection, userId, movieId)) {
                throw new ConflictException("You may only review a film you have rented");
            }

            Review review = new Review();
            review.setMovieId(movieId);
            review.setUserId(userId);
            review.setRating(rating);
            review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
            reviewDao.save(connection, review);
        });
    }

    /**
     * Every review of a title, for the film page.
     *
     * <p>{@code readOnly} rather than {@code runInTransaction}: this only reads,
     * so it needs no transaction to commit and should not take write locks.
     */
    public List<Review> forMovie(long movieId) {
        return Database.readOnly(connection -> reviewDao.findByMovie(connection, movieId));
    }

    /**
     * This customer's own review of a title, if any.
     *
     * <p>Used to pre-fill the form, so that revising an opinion visibly edits
     * the existing review rather than looking like a new one.
     */
    public Optional<Review> byUser(long movieId, long userId) {
        return Database.readOnly(connection -> reviewDao.findByMovieAndUser(connection, movieId, userId));
    }

    /** @return whether this customer is entitled to review this title. */
    public boolean mayReview(long movieId, long userId) {
        return Database.readOnly(connection -> rentalDao.hasEverRentedMovie(connection, userId, movieId));
    }
}
