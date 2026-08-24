package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the paging arithmetic used to build the SQL LIMIT clause.
 *
 * <p>A <em>unit</em> test in the sense that it needs nothing outside the class
 * under test: no database, no server, no network. That is what makes these run
 * in milliseconds and never fail for a reason unrelated to the code, and it is
 * possible here only because the arithmetic lives on a plain object rather than
 * being buried in a query.
 *
 * <p>Written with JUnit 5. Three pieces of it appear throughout:
 * {@code @Test} marks one method as a test the framework should run;
 * {@code @DisplayName} gives that test the readable sentence that appears in
 * the report, which is why the method names can stay short; and the
 * {@code assert...} calls state what must be true, failing the test with the
 * expected and actual values when it is not.
 *
 * <p>Each test builds its own data and makes no assumption about what the
 * others did, so they can be run in any order or on their own.
 */
class MovieSearchCriteriaTest {

    /*
     * The offset is what SQL is told to skip. Page 1 must skip nothing, and
     * an off-by-one here would silently hide the first film of the catalogue.
     */
    @Test
    @DisplayName("the first page starts at offset zero")
    void firstPageOffset() {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setPageSize(12);
        criteria.setPage(1);
        assertEquals(0, criteria.getOffset());
    }

    @Test
    @DisplayName("each page advances the offset by one page size")
    void laterPageOffset() {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setPageSize(12);
        criteria.setPage(3);
        assertEquals(24, criteria.getOffset());
    }

    @Test
    @DisplayName("a page number below one is clamped rather than producing a negative offset")
    void guardsAgainstNegativeOffset() {
        // A negative OFFSET is a SQL syntax error, so this must never be emitted
        // however the page number was arrived at.
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setPageSize(12);
        criteria.setPage(0);
        assertEquals(0, criteria.getOffset());

        criteria.setPage(-5);
        assertEquals(0, criteria.getOffset());
    }

    /*
     * A box the user tabbed through leaves spaces behind. Treating that as a
     * search term would add a WHERE clause matching nothing and show an empty
     * catalogue for what the user experienced as no search at all.
     */
    @Test
    @DisplayName("blank keywords do not count as a search term")
    void blankKeywordIsNoKeyword() {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        assertFalse(criteria.hasKeyword());

        criteria.setKeyword("   ");
        assertFalse(criteria.hasKeyword());

        criteria.setKeyword("matrix");
        assertTrue(criteria.hasKeyword());
    }

    /*
     * Guards the rule described in CatalogBean.search(): narrowing a search
     * while on a later page must not leave the user staring at an empty one.
     */
    @Test
    @DisplayName("changing a filter returns to the first page")
    void resetPaging() {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setPage(7);
        criteria.resetPaging();
        assertEquals(1, criteria.getPage());
    }
}
