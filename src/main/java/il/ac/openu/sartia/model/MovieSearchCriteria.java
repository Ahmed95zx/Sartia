package il.ac.openu.sartia.model;

import java.io.Serializable;

/**
 * The filters a customer chose on the catalogue screen, carried as one object
 * from the JSF page down to the DAO.
 *
 * <p>Passing a criteria object rather than a long parameter list keeps the
 * search signature stable as filters are added, and lets the DAO build the
 * {@code WHERE} clause by inspecting which fields were actually populated -
 * every unset field is simply omitted from the query.
 */
public class MovieSearchCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How results are ordered. Mapped to a fixed column in the DAO - never interpolated. */
    public enum SortBy {
        TITLE, YEAR, RATING, NEWEST
    }

    private String keyword;
    private Long categoryId;
    private Integer yearFrom;
    private Integer yearTo;
    private boolean onlyAvailable;
    private SortBy sortBy = SortBy.TITLE;
    private boolean ascending = true;

    private int page = 1;
    private int pageSize = 12;

    /** @return zero-based offset for the SQL LIMIT clause. */
    public int getOffset() {
        return (Math.max(page, 1) - 1) * pageSize;
    }

    /** @return {@code true} when a non-blank keyword was supplied. */
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    /** Resets paging to the first page - called whenever a filter changes. */
    public void resetPaging() {
        this.page = 1;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public boolean isOnlyAvailable() {
        return onlyAvailable;
    }

    public void setOnlyAvailable(boolean onlyAvailable) {
        this.onlyAvailable = onlyAvailable;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public boolean isAscending() {
        return ascending;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
