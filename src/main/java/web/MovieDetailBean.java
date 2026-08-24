package web;

import model.Movie;
import model.Rental;
import model.Review;
import service.CatalogService;
import service.RentalService;
import service.ReviewService;
import service.exception.BusinessException;
import service.exception.NotFoundException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Backs the film detail screen: full title information, availability, the rent
 * action, and the review form.
 */
@Named("movieBean")
@ViewScoped
public class MovieDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Business layer: reads the title itself. */
    @Inject
    private CatalogService catalogService;

    /** Business layer: performs the rental and answers whether one is allowed. */
    @Inject
    private RentalService rentalService;

    /** Business layer: reads and stores reviews. */
    @Inject
    private ReviewService reviewService;

    /** Who is signed in. Decides which controls the page offers. */
    @Inject
    private SessionBean session;

    /** Title id taken from the query string, e.g. the 3 in /movie.xhtml?id=3. */
    private long movieId;

    /** The title being shown, or {@code null} if the id matched nothing. */
    private Movie movie;

    /** Reviews of this title, for the list under the synopsis. */
    private List<Review> reviews;

    /**
     * Score bound to the review form. Starts at 5 so the stars render filled
     * rather than at an accidental zero.
     */
    private int rating = 5;

    /** Review text bound to the form. */
    private String comment;

    /**
     * Loads the title and everything shown around it.
     *
     * <p>Bound to a {@code <f:viewParam>} on movie.xhtml, which copies the
     * {@code id} query parameter into {@link #setMovieId(long)} and then calls
     * this method before the page renders. That is what makes the screen
     * reachable by a plain bookmarkable URL such as {@code /movie.xhtml?id=3},
     * rather than only by clicking through from the catalogue.
     *
     * <p>Because it runs before every render, it doubles as the refresh used
     * after renting or reviewing: the actions below simply call it again rather
     * than each patching up its own part of the screen.
     *
     * <p>If the customer has already reviewed this title, their existing rating
     * and text are loaded into the form, so writing a second review visibly
     * edits the first rather than appearing to add another.
     */
    public void load() {
        if (movieId <= 0) {
            return;
        }

        try {
            movie = catalogService.findMovie(movieId);
        } catch (NotFoundException missing) {
            // Left null so the page renders its own "film not found" panel with
            // a way back to the catalogue. Allowing this to escape the view
            // action would instead trigger the container's generic error page,
            // which says nothing useful about a mistyped id.
            movie = null;
            return;
        }

        reviews = reviewService.forMovie(movieId);

        if (session.isLoggedIn()) {
            reviewService.byUser(movieId, session.getUserId()).ifPresent(existing -> {
                rating = existing.getRating();
                comment = existing.getComment();
            });
        }
    }

    /**
     * Rents a copy of this title to the signed-in customer.
     *
     * <p>The signed-in check here is for the message, not for the security: it
     * lets the page say something useful instead of failing obscurely. What
     * actually prevents an unauthenticated rental is that the customer id comes
     * from the session, and that the service re-checks every rule inside the
     * transaction.
     *
     * <p>The same applies to {@link #isCanRent()} greying out the button. That
     * answer is read when the page renders and may be stale by the time the
     * button is pressed, so it is a courtesy to the user rather than a control:
     * the last copy can be taken by somebody else in between, and only the
     * transaction can settle who got it.
     */
    public void rent() {
        if (!session.isLoggedIn()) {
            Messages.error("You need to sign in to rent a film");
            return;
        }

        try {
            Rental rental = rentalService.rent(session.getUserId(), movieId);
            Messages.info("\"" + movie.getTitle() + "\" was rented successfully. "
                        + "Please return it by " + rental.getDueDate() + ".");
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
        }

        // Reload either way: on success to show the copy count drop, on failure
        // because the reason for that failure was usually someone else's change.
        load();
    }

    /**
     * Saves the customer's rating and comment.
     *
     * <p>Whether this inserts or updates is not decided here. The reviews table
     * is unique on (movie, user), and the service turns a second submission
     * from the same customer into an update of their existing row, so a
     * customer cannot stack several opinions on one title.
     */
    public void submitReview() {
        if (!session.isLoggedIn()) {
            Messages.error("You need to sign in to write a review");
            return;
        }

        try {
            reviewService.submit(movieId, session.getUserId(), rating, comment);
            Messages.info("Your review was saved. Thank you!");
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
        }
        load();
    }

    /** @return whether to render the rent button as enabled. */
    public boolean isCanRent() {
        return session.isLoggedIn()
               && movie != null
               && rentalService.canRent(session.getUserId(), movieId);
    }

    /** @return whether the review form should be shown at all. */
    public boolean isMayReview() {
        return session.isLoggedIn() && reviewService.mayReview(movieId, session.getUserId());
    }

    /* Read and written by movie.xhtml through #{movieBean...}. The setter for
     * movieId is what <f:viewParam> calls with the id from the query string. */

    public long getMovieId() {
        return movieId;
    }

    public void setMovieId(long movieId) {
        this.movieId = movieId;
    }

    public Movie getMovie() {
        return movie;
    }

    public List<Review> getReviews() {
        return reviews;
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
}
