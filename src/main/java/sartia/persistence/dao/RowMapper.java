package sartia.persistence.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link ResultSet} row into a domain object.
 *
 * <p>Every DAO defines its mappers as constants and reads rows through
 * {@link #list} / {@link #single}, which keeps cursor handling in one place
 * instead of repeating {@code while (rs.next())} in a dozen methods.
 *
 * @param <T> the domain type produced
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the row the result set is currently positioned on. Implementations
     * must not call {@link ResultSet#next()}.
     */
    T map(ResultSet rs) throws SQLException;

    /** Drains the result set into a list, mapping every row. */
    static <T> List<T> list(ResultSet rs, RowMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        while (rs.next()) {
            results.add(mapper.map(rs));
        }
        return results;
    }

    /** Maps the first row if there is one. Any further rows are ignored. */
    static <T> Optional<T> single(ResultSet rs, RowMapper<T> mapper) throws SQLException {
        return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
    }

    /* ----------------------------------------------------------------
     * Null-aware column readers.
     *
     * ResultSet.getInt returns 0 both for the value 0 and for SQL NULL.
     * For nullable columns such as release_year that distinction matters,
     * so these helpers consult wasNull() and hand back a boxed null.
     * ---------------------------------------------------------------- */

    static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    static LocalDate date(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }
}
