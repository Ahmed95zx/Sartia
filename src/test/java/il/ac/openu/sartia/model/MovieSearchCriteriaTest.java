package il.ac.openu.sartia.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the paging arithmetic used to build the SQL LIMIT clause. */
class MovieSearchCriteriaTest {

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

    @Test
    @DisplayName("changing a filter returns to the first page")
    void resetPaging() {
        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setPage(7);
        criteria.resetPaging();
        assertEquals(1, criteria.getPage());
    }
}
