package il.ac.openu.sartia.service;

import il.ac.openu.sartia.dao.Database;
import il.ac.openu.sartia.dao.RentalDao;
import il.ac.openu.sartia.dao.ReviewDao;
import il.ac.openu.sartia.model.Review;
import il.ac.openu.sartia.service.exception.ConflictException;
import il.ac.openu.sartia.service.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Customer ratings and comments on titles. */
@ApplicationScoped
public class ReviewService {

    private static final int MAX_COMMENT_LENGTH = 2000;

    private final ReviewDao reviewDao = new ReviewDao();
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

    public List<Review> forMovie(long movieId) {
        return Database.readOnly(connection -> reviewDao.findByMovie(connection, movieId));
    }

    public Optional<Review> byUser(long movieId, long userId) {
        return Database.readOnly(connection -> reviewDao.findByMovieAndUser(connection, movieId, userId));
    }

    /** @return whether this customer is entitled to review this title. */
    public boolean mayReview(long movieId, long userId) {
        return Database.readOnly(connection -> rentalDao.hasEverRentedMovie(connection, userId, movieId));
    }
}
