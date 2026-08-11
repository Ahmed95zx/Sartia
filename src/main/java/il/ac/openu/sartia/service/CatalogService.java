package il.ac.openu.sartia.service;

import il.ac.openu.sartia.dao.CategoryDao;
import il.ac.openu.sartia.dao.CopyDao;
import il.ac.openu.sartia.dao.Database;
import il.ac.openu.sartia.dao.MovieDao;
import il.ac.openu.sartia.dao.RentalDao;
import il.ac.openu.sartia.model.Category;
import il.ac.openu.sartia.model.Copy;
import il.ac.openu.sartia.model.CopyStatus;
import il.ac.openu.sartia.model.Movie;
import il.ac.openu.sartia.model.MovieSearchCriteria;
import il.ac.openu.sartia.service.exception.ConflictException;
import il.ac.openu.sartia.service.exception.NotFoundException;
import il.ac.openu.sartia.service.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/**
 * Browsing and searching the catalogue, plus the administrator's catalogue and
 * inventory maintenance.
 */
@ApplicationScoped
public class CatalogService {

    /** Cinema predates 1888, so anything earlier is a typo rather than a title. */
    private static final int EARLIEST_PLAUSIBLE_YEAR = 1888;

    private final MovieDao movieDao = new MovieDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final CopyDao copyDao = new CopyDao();
    private final RentalDao rentalDao = new RentalDao();

    /**
     * One page of search results.
     *
     * <p>Written as a class with JavaBean getters rather than as a record,
     * because the JSF pages read it through the Expression Language and EL
     * resolves a property by looking for {@code getX()}. A record exposes
     * {@code x()} instead, which EL reports as a missing property.
     *
     * <p>There is deliberately no {@code isEmpty()}: {@code empty} is a
     * reserved operator in EL, so {@code #{result.empty}} fails to parse.
     * Pages test {@code #{empty result.movies}} instead.
     *
     * <p>Serializable because a {@code @ViewScoped} bean holds one, and the
     * container may write such a bean out when it passivates the session.
     */
    public static final class SearchResult implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private final List<Movie> movies;
        private final int totalCount;
        private final int page;
        private final int pageSize;

        SearchResult(List<Movie> movies, int totalCount, int page, int pageSize) {
            this.movies = movies;
            this.totalCount = totalCount;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<Movie> getMovies() {
            return movies;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotalPages() {
            return pageSize <= 0 ? 1 : Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        }

        public boolean isHasPrevious() {
            return page > 1;
        }

        public boolean isHasNext() {
            return page < getTotalPages();
        }
    }

    /**
     * Runs a catalogue search.
     *
     * <p>The result rows and the total count are read in a single connection so
     * the page contents and the page count describe the same snapshot of the
     * catalogue.
     */
    public SearchResult search(MovieSearchCriteria criteria) {
        return Database.readOnly(connection -> {
            List<Movie> movies = movieDao.search(connection, criteria);
            int total = movieDao.countMatching(connection, criteria);
            return new SearchResult(movies, total, criteria.getPage(), criteria.getPageSize());
        });
    }

    public Movie findMovie(long id) {
        return Database.readOnly(connection -> movieDao.findById(connection, id))
                .orElseThrow(() -> NotFoundException.movie(id));
    }

    public List<Category> categories() {
        return Database.readOnly(categoryDao::findAll);
    }

    public List<Category> categoriesWithCounts() {
        return Database.readOnly(categoryDao::findAllWithCounts);
    }

    public List<Copy> copiesOf(long movieId) {
        return Database.readOnly(connection -> copyDao.findByMovie(connection, movieId));
    }

    /* ------------------------------------------------------------------
     * Administration
     * ------------------------------------------------------------------ */

    /**
     * Adds a title and stocks it with an initial run of copies.
     *
     * <p>Title and copies are created in the same transaction so the catalogue
     * never shows a film that has no discs behind it.
     *
     * @param initialCopies how many discs to stock, at least one
     */
    public Movie createMovie(Movie movie, int initialCopies) {
        validate(movie);
        if (initialCopies < 1) {
            throw new ValidationException("יש להזין לפחות עותק אחד");
        }

        return Database.inTransaction(connection -> {
            movieDao.insert(connection, movie);
            for (int i = 1; i <= initialCopies; i++) {
                copyDao.insert(connection, new Copy(movie.getId(), barcodeFor(movie.getId(), i)));
            }
            return movie;
        });
    }

    public void updateMovie(Movie movie) {
        validate(movie);
        Database.runInTransaction(connection -> movieDao.update(connection, movie));
    }

    /**
     * Adds discs to a title already in the catalogue.
     *
     * <p>Barcodes continue from the highest sequence already issued rather than
     * restarting at one, which would collide with the existing unique barcodes.
     */
    public void addCopies(long movieId, int count) {
        if (count < 1) {
            throw new ValidationException("יש להזין מספר עותקים חיובי");
        }

        Database.runInTransaction(connection -> {
            movieDao.findById(connection, movieId)
                    .orElseThrow(() -> NotFoundException.movie(movieId));

            int existing = copyDao.maxBarcodeSequence(connection, movieId);
            for (int i = 1; i <= count; i++) {
                copyDao.insert(connection, new Copy(movieId, barcodeFor(movieId, existing + i)));
            }
        });
    }

    /**
     * Removes a title from the catalogue.
     *
     * <p>Deleting a title cascades its copies away, but {@code fk_rentals_copy}
     * is RESTRICT - so the database refuses the delete outright if any rental
     * row, open <em>or already returned</em>, still references one of those
     * copies. Rental history is deliberately permanent, which means any title
     * that has ever been lent cannot be deleted.
     *
     * <p>Both conditions are therefore checked here and reported as clear
     * messages. Without the second check the database would reject the delete
     * with a foreign-key error, which surfaces as a
     * {@link il.ac.openu.sartia.dao.DataAccessException} and an error page
     * rather than an explanation.
     */
    public void deleteMovie(long movieId) {
        Database.runInTransaction(connection -> {
            Movie movie = movieDao.findById(connection, movieId)
                    .orElseThrow(() -> NotFoundException.movie(movieId));

            boolean anyOut = copyDao.findByMovie(connection, movieId).stream()
                    .anyMatch(copy -> copy.getStatus() == CopyStatus.RENTED);
            if (anyOut) {
                throw new ConflictException(
                        "לא ניתן למחוק את \"" + movie.getTitle() + "\" בזמן שעותקים ממנו מושאלים.");
            }

            if (rentalDao.countAnyRentalsOfMovie(connection, movieId) > 0) {
                throw new ConflictException(
                        "לא ניתן למחוק את \"" + movie.getTitle() + "\" משום שקיימות עבורו רשומות השאלה. "
                        + "היסטוריית ההשאלות נשמרת לצמיתות.");
            }

            movieDao.delete(connection, movieId);
        });
    }

    /** Human-readable, unique per disc: {@code SRT-<movieId>-<sequence>}. */
    private String barcodeFor(long movieId, int sequence) {
        return String.format("SRT-%d-%03d", movieId, sequence);
    }

    private void validate(Movie movie) {
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            throw new ValidationException("יש להזין שם סרט");
        }
        if (movie.getCategoryId() <= 0) {
            throw new ValidationException("יש לבחור קטגוריה");
        }

        Integer year = movie.getReleaseYear();
        if (year != null && (year < EARLIEST_PLAUSIBLE_YEAR || year > Year.now().getValue() + 1)) {
            throw new ValidationException(
                    "שנת הוצאה חייבת להיות בין " + EARLIEST_PLAUSIBLE_YEAR + " ל-" + (Year.now().getValue() + 1));
        }

        Integer duration = movie.getDurationMinutes();
        if (duration != null && (duration <= 0 || duration > 600)) {
            throw new ValidationException("אורך הסרט חייב להיות בין 1 ל-600 דקות");
        }

        BigDecimal price = movie.getDailyPrice();
        if (price == null || price.signum() < 0) {
            throw new ValidationException("מחיר יומי חייב להיות מספר אי-שלילי");
        }
    }
}
