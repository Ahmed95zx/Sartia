package model;

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

    /**
     * Version stamp used when Java turns an object of this class into bytes.
     *
     * <p>These objects are held in JSF view and session scope, and a servlet
     * container is allowed to serialise that state: to hand a session to
     * another server, or to keep it across a restart. Fixing the number by
     * hand means state written by an earlier build can still be read back
     * after a field is added, instead of failing on a version mismatch.
     */
    private static final long serialVersionUID = 1L;

    /** How results are ordered. Mapped to a fixed column in the DAO - never interpolated. */
    public enum SortBy {
        TITLE, YEAR, RATING, NEWEST
    }

    /** Free text typed in the search box. Matched against title and director. */
    private String keyword;

    /**
     * Genre filter, or {@code null} for "all categories".
     *
     * <p>Boxed {@code Long} exactly so that {@code null} can mean "no filter".
     * A primitive {@code long} would have to use 0 as a magic value instead.
     */
    private Long categoryId;

    /** Earliest release year to include, or {@code null} for no lower bound. */
    private Integer yearFrom;

    /** Latest release year to include, or {@code null} for no upper bound. */
    private Integer yearTo;

    /** When ticked, hides titles with no copy currently on the shelf. */
    private boolean onlyAvailable;

    /** Which column to order by. Defaults to alphabetical. */
    private SortBy sortBy = SortBy.TITLE;

    /** Direction of that ordering. */
    private boolean ascending = true;

    /** Page currently being viewed, counted from 1 as the buttons show it. */
    private int page = 1;

    /** Rows per page. Twelve fills the catalogue grid exactly. */
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

    /* ------------------------------------------------------------------
     * Accessors.
     *
     * JSF binds the search form straight to these: each input on
     * catalog.xhtml writes one field here when the form is submitted.
     *
     * They carry no logic of their own, so they are described here as a group
     * rather than repeating the same sentence above each one.
     * ------------------------------------------------------------------ */

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
