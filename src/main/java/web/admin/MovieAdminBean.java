package web.admin;

import model.Category;
import model.Copy;
import model.Movie;
import model.MovieSearchCriteria;
import service.CatalogService;
import service.exception.BusinessException;
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
 */
@Named("movieAdminBean")
@ViewScoped
public class MovieAdminBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Deliberately large: an administrator wants the whole catalogue on one screen. */
    private static final int ADMIN_PAGE_SIZE = 200;

    @Inject
    private CatalogService catalogService;

    private List<Movie> movies;
    private List<Category> categories;

    /** The title currently open in the editor; a fresh object means "new title". */
    private Movie editing = new Movie();
    private int initialCopies = 1;
    private boolean editorOpen;

    /** Copies of the title whose inventory panel is expanded. */
    private List<Copy> inventory;
    private long inventoryMovieId;
    private int copiesToAdd = 1;

    @PostConstruct
    public void init() {
        categories = catalogService.categories();
        reload();
    }

    /* ---------------- editor ---------------- */

    public void newMovie() {
        editing = new Movie();
        initialCopies = 1;
        editorOpen = true;
    }

    public void edit(Movie movie) {
        // Edit a copy, so cancelling leaves the row in the table untouched.
        editing = copyOf(movie);
        editorOpen = true;
    }

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

    public void showInventory(Movie movie) {
        inventoryMovieId = movie.getId();
        inventory = catalogService.copiesOf(movie.getId());
        copiesToAdd = 1;
    }

    public void closeInventory() {
        inventoryMovieId = 0;
        inventory = null;
    }

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

    /* ---------------- helpers ---------------- */

    private void reload() {
        MovieSearchCriteria all = new MovieSearchCriteria();
        all.setPageSize(ADMIN_PAGE_SIZE);
        movies = catalogService.search(all).getMovies();
    }

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

    private void info(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, null));
    }

    private void error(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }

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
