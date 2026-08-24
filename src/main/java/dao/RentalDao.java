package dao;

import model.Rental;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes {@code rentals}.
 *
 * <p>A rental row is the record that a copy left the shelf. It is never
 * deleted - returning a disc closes the row by stamping {@code returned_at},
 * which preserves the rental history the specification requires customers to
 * be able to review.
 */
public class RentalDao {

    private static final RowMapper<Rental> MAPPER = rs -> {
        Rental rental = new Rental();
        rental.setId(rs.getLong("id"));
        rental.setCopyId(rs.getLong("copy_id"));
        rental.setUserId(rs.getLong("user_id"));
        rental.setRentedAt(RowMapper.dateTime(rs, "rented_at"));
        rental.setDueDate(RowMapper.date(rs, "due_date"));
        rental.setReturnedAt(RowMapper.dateTime(rs, "returned_at"));
        rental.setLateFee(rs.getBigDecimal("late_fee"));
        rental.setMovieId(rs.getLong("movie_id"));
        rental.setMovieTitle(rs.getString("movie_title"));
        rental.setBarcode(rs.getString("barcode"));
        rental.setUserFullName(rs.getString("user_full_name"));
        rental.setUsername(rs.getString("username"));
        return rental;
    };

    private static final String SELECT_RENTAL = """
            SELECT r.id, r.copy_id, r.user_id, r.rented_at, r.due_date, r.returned_at, r.late_fee,
                   cp.barcode, cp.movie_id,
                   m.title      AS movie_title,
                   u.full_name  AS user_full_name,
                   u.username
            FROM   rentals r
            JOIN   copies cp ON cp.id = r.copy_id
            JOIN   movies m  ON m.id  = cp.movie_id
            JOIN   users  u  ON u.id  = r.user_id
            """;

    /**
     * Opens a rental.
     *
     * <p>If the copy already has an open rental this insert violates the unique
     * index {@code uq_rentals_copy_active} and throws. That is intentional: it
     * is the last line of defence behind the row lock taken in
     * {@link CopyDao#lockAvailableCopy}, and it holds even if a future caller
     * forgets to take that lock.
     */
    public Rental insert(Connection connection, long copyId, long userId, LocalDate dueDate) throws SQLException {
        String sql = "INSERT INTO rentals (copy_id, user_id, due_date) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, copyId);
            statement.setLong(2, userId);
            statement.setDate(3, Date.valueOf(dueDate));
            statement.executeUpdate();

            Rental rental = new Rental();
            rental.setCopyId(copyId);
            rental.setUserId(userId);
            rental.setDueDate(dueDate);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    rental.setId(keys.getLong(1));
                }
            }
            return rental;
        }
    }

    /**
     * Closes an open rental.
     *
     * <p>The {@code returned_at IS NULL} predicate makes this idempotent: a
     * double submit, or two staff members processing the same return at once,
     * updates zero rows the second time instead of overwriting the original
     * return timestamp.
     *
     * @return {@code true} if this call was the one that closed the rental
     */
    public boolean close(Connection connection, long rentalId, BigDecimal lateFee) throws SQLException {
        String sql = "UPDATE rentals SET returned_at = CURRENT_TIMESTAMP, late_fee = ? "
                   + "WHERE id = ? AND returned_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, lateFee);
            statement.setLong(2, rentalId);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<Rental> findById(Connection connection, long id) throws SQLException {
        String sql = SELECT_RENTAL + " WHERE r.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.single(rs, MAPPER);
            }
        }
    }

    /** Full rental history for one customer, newest first. */
    public List<Rental> findByUser(Connection connection, long userId) throws SQLException {
        String sql = SELECT_RENTAL + " WHERE r.user_id = ? ORDER BY r.rented_at DESC";
        return queryList(connection, sql, userId);
    }

    /** Only the discs the customer currently holds. */
    public List<Rental> findOpenByUser(Connection connection, long userId) throws SQLException {
        String sql = SELECT_RENTAL + " WHERE r.user_id = ? AND r.returned_at IS NULL ORDER BY r.due_date";
        return queryList(connection, sql, userId);
    }

    /** Every disc currently out, for the administrator's returns screen. */
    public List<Rental> findAllOpen(Connection connection) throws SQLException {
        String sql = SELECT_RENTAL + " WHERE r.returned_at IS NULL ORDER BY r.due_date";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return RowMapper.list(rs, MAPPER);
        }
    }

    /** Open rentals already past their due date. */
    public List<Rental> findOverdue(Connection connection) throws SQLException {
        String sql = SELECT_RENTAL + " WHERE r.returned_at IS NULL AND r.due_date < CURRENT_DATE "
                   + "ORDER BY r.due_date";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return RowMapper.list(rs, MAPPER);
        }
    }

    /** How many discs the customer is holding - checked against the borrowing limit. */
    public int countOpenByUser(Connection connection, long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM rentals WHERE user_id = ? AND returned_at IS NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * @return {@code true} if the customer already holds a copy of this title.
     *         Renting a second copy of a film you already have is almost always
     *         a misclick, so the service rejects it.
     */
    public boolean hasOpenRentalOfMovie(Connection connection, long userId, long movieId) throws SQLException {
        String sql = """
                SELECT 1
                FROM   rentals r
                JOIN   copies cp ON cp.id = r.copy_id
                WHERE  r.user_id = ? AND cp.movie_id = ? AND r.returned_at IS NULL
                LIMIT  1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * @return how many rental rows exist for any copy of this title, including
     *         rentals already returned. Used before deleting a title: rental
     *         history is permanent, so a title that has ever been lent cannot
     *         be removed from the catalogue.
     */
    public int countAnyRentalsOfMovie(Connection connection, long movieId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM   rentals r
                JOIN   copies cp ON cp.id = r.copy_id
                WHERE  cp.movie_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** @return {@code true} if the customer ever rented this title - gates reviewing it. */
    public boolean hasEverRentedMovie(Connection connection, long userId, long movieId) throws SQLException {
        String sql = """
                SELECT 1
                FROM   rentals r
                JOIN   copies cp ON cp.id = r.copy_id
                WHERE  r.user_id = ? AND cp.movie_id = ?
                LIMIT  1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Rental> queryList(Connection connection, String sql, long parameter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.list(rs, MAPPER);
            }
        }
    }
}
