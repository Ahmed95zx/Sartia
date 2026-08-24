package web;

import model.Category;
import model.MovieSearchCriteria;
import service.CatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * Backs the catalogue screen: category browsing, keyword search, filtering,
 * sorting and paging.
 *
 * <p>View-scoped so the customer's filters survive while they page through
 * results and disappear when they leave the screen. Request scope would reset
 * the filters on every page click; session scope would still be applying them
 * when the customer came back tomorrow.
 */
@Named("catalogBean")
@ViewScoped
public class CatalogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Business layer: runs the search and counts the categories. */
    @Inject
    private CatalogService catalogService;

    /**
     * Every filter the customer has chosen, in one object.
     *
     * <p>The page binds its inputs straight to the fields of this object, so a
     * new filter means adding a field there and an input on the page, without
     * touching the method signatures in between.
     */
    private MovieSearchCriteria criteria = new MovieSearchCriteria();

    /** The current page of results, together with the total count and paging flags. */
    private CatalogService.SearchResult result;

    /** Sidebar entries, each carrying how many titles it holds. */
    private List<Category> categories;

    /**
     * Loads the sidebar once and runs the first, unfiltered search.
     *
     * <p>Runs after CDI has filled the injected fields; a constructor would be
     * too early, as {@code catalogService} would still be {@code null}. The
     * categories are read once here rather than on every search, because they
     * do not change while the customer is on the page.
     */
    @PostConstruct
    public void init() {
        categories = catalogService.categoriesWithCounts();
        runSearch();
    }

    /**
     * Applies changed filters, always restarting from page one.
     *
     * <p>Resetting the page matters: a customer sitting on page 5 who then
     * narrows the search to three results would otherwise be shown an empty
     * page 5 and conclude that nothing matched.
     */
    public void search() {
        criteria.resetPaging();
        runSearch();
    }

    /** Clears every filter and shows the whole catalogue again. */
    public void clearFilters() {
        criteria = new MovieSearchCriteria();
        runSearch();
    }

    /** Jumps to a category from the sidebar. */
    public void filterByCategory(Long categoryId) {
        criteria.setCategoryId(categoryId);
        criteria.resetPaging();
        runSearch();
    }

    /**
     * Moves forward one page, if there is one.
     *
     * <p>The guard is not only about the button being hidden. A page can be
     * re-submitted from a stale screen after the catalogue has changed, so the
     * bean re-checks rather than trusting that the page would not have asked.
     */
    public void nextPage() {
        if (result != null && result.isHasNext()) {
            criteria.setPage(criteria.getPage() + 1);
            runSearch();
        }
    }

    /** Moves back one page, if there is one. Guarded as {@link #nextPage()} is. */
    public void previousPage() {
        if (result != null && result.isHasPrevious()) {
            criteria.setPage(criteria.getPage() - 1);
            runSearch();
        }
    }

    /**
     * Re-sorts the results. Clicking the column already sorted by flips the
     * direction, which is what a user expects from a sortable header.
     */
    public void sortBy(String sortName) {
        MovieSearchCriteria.SortBy requested = MovieSearchCriteria.SortBy.valueOf(sortName);
        if (criteria.getSortBy() == requested) {
            criteria.setAscending(!criteria.isAscending());
        } else {
            criteria.setSortBy(requested);
            criteria.setAscending(true);
        }
        criteria.resetPaging();
        runSearch();
    }

    /**
     * Runs the query for the current criteria.
     *
     * <p>Every action above ends here rather than each doing its own query,
     * so there is one place where searching happens and no way for a new
     * action to forget a step.
     */
    private void runSearch() {
        result = catalogService.search(criteria);
    }

    /* Read by catalog.xhtml through #{catalogBean...}. */

    public MovieSearchCriteria getCriteria() {
        return criteria;
    }

    public CatalogService.SearchResult getResult() {
        return result;
    }

    public List<Category> getCategories() {
        return categories;
    }

    /** Name of the active category, for the "showing results in ..." heading. */
    public String getActiveCategoryName() {
        if (criteria.getCategoryId() == null) {
            return null;
        }
        return categories.stream()
                .filter(category -> category.getId() == criteria.getCategoryId())
                .map(Category::getName)
                .findFirst()
                .orElse(null);
    }
}
