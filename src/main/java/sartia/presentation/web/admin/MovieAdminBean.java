package sartia.presentation.web.admin;

import sartia.business.domain.Category;
import sartia.business.domain.Copy;
import sartia.business.domain.Movie;
import sartia.business.domain.MovieSearchCriteria;
import sartia.business.service.CatalogService;
import sartia.business.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Backs the administrator's catalogue screen: listing titles, adding and
 * editing them, stocking extra copies and removing titles.
 *
 * <p>The screen has two independent panels, and the fields below are grouped
 * the same way: an editor for the details of one title, and an inventory panel
 * listing the physical discs of one title. Either can be open without the
 * other, which is why each keeps its own state.
 *
 * <p>Reachable only through {@code AuthFilter}, which refuses {@code /admin/*}
 * to anyone without the ADMIN role.
 */
@Named("movieAdminBean")
@ViewScoped
public class MovieAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Deliberately large: an administrator wants the whole catalogue on one screen. */
    private static final int ADMIN_PAGE_SIZE = 200;

    /** Business layer: every catalogue read and write on this screen. */
    @Inject
    private CatalogService catalogService;

    /** The whole catalogue, as listed in the table. */
    private List<Movie> movies;

    /** Genres, for the drop-down in the editor. Loaded once. */
    private List<Category> categories;

    /** The title currently open in the editor; a fresh object means "new title". */
    private Movie editing = new Movie();
    /** How many discs to stock when a brand new title is saved. */
    private int initialCopies = 1;

    /** Whether the editor panel is showing. Bound to the panel's rendered flag. */
    private boolean editorOpen;

    /** Copies of the title whose inventory panel is expanded. */
    private List<Copy> inventory;
    /** Which title the inventory panel is showing, or 0 when it is closed. */
    private long inventoryMovieId;

    /** How many further discs to stock, bound to the input in that panel. */
    private int copiesToAdd = 1;

    /**
     * Loads the genre list once and the catalogue table, after CDI has filled
     * the injected fields.
     */
    @PostConstruct
    public void init() {
        categories = catalogService.categories();
        reload();
    }

    /* ---------------- editor ---------------- */

    /**
     * Opens the editor on a blank title.
     *
     * <p>A {@link Movie} whose id is still 0 is what {@link #save()} reads as
     * "this is new", so there is no separate flag to keep in step.
     */
    public void newMovie() {
        editing = new Movie();
        initialCopies = 1;
        editorOpen = true;
    }

    /** Opens the editor on an existing title. */
    public void edit(Movie movie) {
        // Edit a copy, so cancelling leaves the row in the table untouched.
        editing = copyOf(movie);
        editorOpen = true;
    }

    /** Closes the editor and discards whatever was being typed. */
    public void cancelEdit() {
        editing = new Movie();
        editorOpen = false;
    }

    /** Creates or updates, depending on whether the edited title has an id yet. */
    public void save() {
        try {
            if (editing.getId() == 0) {
                catalogService.createMovie(editing, initialCopies);
                info("\"" + editing.getTitle() + "\" was added with " + initialCopies + " copies");
            } else {
                catalogService.updateMovie(editing);
                info("\"" + editing.getTitle() + "\" was updated");
            }
            editorOpen = false;
            editing = new Movie();
            reload();
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
    }

    /**
     * Deletes a title from the catalogue.
     *
     * <p>Refused by the service when the title has any rental record at all,
     * returned loans included, because deleting it would break the foreign key
     * those rows depend on and lose the shop's history with it.
     */
    public void delete(Movie movie) {
        try {
            catalogService.deleteMovie(movie.getId());
            info("\"" + movie.getTitle() + "\" was deleted");
            reload();
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
    }

    /* ---------------- inventory ---------------- */

    /** Expands the inventory panel for one title, listing its physical discs. */
    public void showInventory(Movie movie) {
        inventoryMovieId = movie.getId();
        inventory = catalogService.copiesOf(movie.getId());
        copiesToAdd = 1;
    }

    /** Collapses the inventory panel. */
    public void closeInventory() {
        inventoryMovieId = 0;
        inventory = null;
    }

    /**
     * Stocks further discs of the open title.
     *
     * <p>Both lists are re-read afterwards: the panel to show the new discs
     * and their generated barcodes, and the table because its availability
     * counts have just changed.
     */
    public void addCopies() {
        try {
            catalogService.addCopies(inventoryMovieId, copiesToAdd);
            info(copiesToAdd + " copies were added");
            inventory = catalogService.copiesOf(inventoryMovieId);
            reload();
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
    }

    /**
     * Discards one disc of the open title - a copy broken, lost for good or
     * sold off.
     *
     * <p>Refused by the service while the disc is out with a customer, once it
     * appears in any rental record, and when it is the title's last copy; the
     * button in the row is only a first filter, and the message shown here is
     * the service's own explanation.
     *
     * <p>Both lists are re-read afterwards for the same reason as in
     * {@link #addCopies()}: the panel to drop the row, and the table because
     * its availability counts have just changed.
     */
    public void removeCopy(Copy copy) {
        try {
            catalogService.removeCopy(copy.getId());
            info("Copy " + copy.getBarcode() + " was removed");
            inventory = catalogService.copiesOf(inventoryMovieId);
            reload();
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
    }

    /* ---------------- helpers ---------------- */

    /**
     * Re-reads the catalogue table.
     *
     * <p>Reuses the customer-facing search with no filters set and a large page
     * size, rather than adding a second "fetch everything" query beside it.
     */
    private void reload() {
        MovieSearchCriteria all = new MovieSearchCriteria();
        all.setPageSize(ADMIN_PAGE_SIZE);
        movies = catalogService.search(all).getMovies();
    }

    /**
     * Copies the editable fields of a title into a detached object.
     *
     * <p>The editor works on this copy so that typing in the form does not
     * alter the row still being displayed in the table behind it: pressing
     * cancel then really does leave the listing as it was.
     *
     * <p>Only the stored columns are copied. The derived ones - copy counts and
     * rating - are deliberately left out, because they are results of other
     * tables and are not the editor's to change.
     */
    private Movie copyOf(Movie source) {
        Movie target = new Movie();
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setDirector(source.getDirector());
        target.setReleaseYear(source.getReleaseYear());
        target.setDurationMinutes(source.getDurationMinutes());
        target.setCategoryId(source.getCategoryId());
        target.setCoverUrl(source.getCoverUrl());
        target.setDailyPrice(source.getDailyPrice());
        return target;
    }

    /** Adds a success notice to the page. */
    private void info(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, null));
    }

    /** Adds a failure notice to the page. */
    private void error(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }

    /* Read and written by admin/movies.xhtml through #{movieAdminBean...}. */

    public List<Movie> getMovies() {
        return movies;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Movie getEditing() {
        return editing;
    }

    public boolean isEditorOpen() {
        return editorOpen;
    }

    public int getInitialCopies() {
        return initialCopies;
    }

    public void setInitialCopies(int initialCopies) {
        this.initialCopies = initialCopies;
    }

    public List<Copy> getInventory() {
        return inventory;
    }

    public long getInventoryMovieId() {
        return inventoryMovieId;
    }

    public int getCopiesToAdd() {
        return copiesToAdd;
    }

    public void setCopiesToAdd(int copiesToAdd) {
        this.copiesToAdd = copiesToAdd;
    }
}
