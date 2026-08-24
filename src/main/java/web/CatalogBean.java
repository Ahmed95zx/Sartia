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

    @Inject
    private CatalogService catalogService;

    private MovieSearchCriteria criteria = new MovieSearchCriteria();
    private CatalogService.SearchResult result;
    private List<Category> categories;

    @PostConstruct
    public void init() {
        categories = catalogService.categoriesWithCounts();
        runSearch();
    }

    /** Applies changed filters, always restarting from page one. */
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

    public void nextPage() {
        if (result != null && result.isHasNext()) {
            criteria.setPage(criteria.getPage() + 1);
            runSearch();
        }
    }

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

    private void runSearch() {
        result = catalogService.search(criteria);
    }

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
