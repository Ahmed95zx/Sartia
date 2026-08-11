package il.ac.openu.sartia.web;

import il.ac.openu.sartia.model.Movie;
import il.ac.openu.sartia.model.Rental;
import il.ac.openu.sartia.model.Review;
import il.ac.openu.sartia.service.CatalogService;
import il.ac.openu.sartia.service.RentalService;
import il.ac.openu.sartia.service.ReviewService;
import il.ac.openu.sartia.service.exception.BusinessException;
import il.ac.openu.sartia.service.exception.NotFoundException;
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

    @Inject
    private CatalogService catalogService;

    @Inject
    private RentalService rentalService;

    @Inject
    private ReviewService reviewService;

    @Inject
    private SessionBean session;

    private long movieId;
    private Movie movie;
    private List<Review> reviews;

    private int rating = 5;
    private String comment;

    /**
     * Loads the title. Bound to a {@code <f:viewParam>} so the screen is
     * reachable by a plain bookmarkable URL such as
     * {@code /movie.xhtml?id=3} rather than only by clicking through.
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

    /** Rents a copy of this title to the signed-in customer. */
    public void rent() {
        if (!session.isLoggedIn()) {
            Messages.error("יש להתחבר כדי להשאיל סרט");
            return;
        }

        try {
            Rental rental = rentalService.rent(session.getUserId(), movieId);
            Messages.info("הסרט \"" + movie.getTitle() + "\" הושאל בהצלחה. "
                        + "יש להחזירו עד " + rental.getDueDate() + ".");
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
        }

        // Reload either way: on success to show the copy count drop, on failure
        // because the reason for that failure was usually someone else's change.
        load();
    }

    /** Saves the customer's rating and comment. */
    public void submitReview() {
        if (!session.isLoggedIn()) {
            Messages.error("יש להתחבר כדי לכתוב ביקורת");
            return;
        }

        try {
            reviewService.submit(movieId, session.getUserId(), rating, comment);
            Messages.info("הביקורת נשמרה. תודה!");
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
