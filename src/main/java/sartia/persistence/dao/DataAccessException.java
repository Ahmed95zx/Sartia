package sartia.persistence.dao;

/**
 * Wraps a {@link java.sql.SQLException} so that layers above the DAO do not
 * have to import JDBC types or handle checked SQL exceptions.
 *
 * <p>This is a deliberate boundary: the service layer is written against the
 * domain, not against a particular persistence mechanism, and a caller should
 * never have to distinguish "the query was malformed" from "the network blipped"
 * in order to render an error page.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
