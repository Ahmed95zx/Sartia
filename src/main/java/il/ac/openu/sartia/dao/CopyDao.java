package il.ac.openu.sartia.dao;

import il.ac.openu.sartia.model.Copy;
import il.ac.openu.sartia.model.CopyStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.OptionalLong;

/**
 * Reads and writes {@code copies} - the physical inventory.
 *
 * <p>This DAO carries the system's most important query,
 * {@link #lockAvailableCopy}, which is what stops two customers renting the
 * same disc.
 */
public class CopyDao {

    private static final RowMapper<Copy> MAPPER = rs -> {
        Copy copy = new Copy();
        copy.setId(rs.getLong("id"));
        copy.setMovieId(rs.getLong("movie_id"));
        copy.setBarcode(rs.getString("barcode"));
        copy.setStatus(CopyStatus.valueOf(rs.getString("status")));
        copy.setAcquiredAt(RowMapper.dateTime(rs, "acquired_at"));
        return copy;
    };

    /**
     * Claims one available copy of a title for the calling transaction.
     *
     * <p>The two clauses on the end are what make this safe:
     * <ul>
     *   <li>{@code FOR UPDATE} takes an exclusive row lock, held until the
     *       surrounding transaction commits. A second transaction cannot select
     *       the same row for update in the meantime, so it cannot decide that
     *       this disc is free.</li>
     *   <li>{@code SKIP LOCKED} tells InnoDB to step over rows another
     *       transaction has already locked instead of waiting for them. Without
     *       it, ten customers renting a popular title would serialise into a
     *       queue, each waiting out the one in front; with it, each simply takes
     *       the next free disc.</li>
     * </ul>
     *
     * <p>The caller must be inside a transaction - see
     * {@link Database#inTransaction}. Called with auto-commit on, the lock would
     * be released the instant this method returned and would protect nothing.
     *
     * @return id of a copy now locked for this transaction, or empty if the
     *         title has no free disc
     */
    public OptionalLong lockAvailableCopy(Connection connection, long movieId) throws SQLException {
        String sql = """
                SELECT id
                FROM   copies
                WHERE  movie_id = ? AND status = 'AVAILABLE'
                ORDER BY id
                LIMIT  1
                FOR UPDATE SKIP LOCKED
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? OptionalLong.of(rs.getLong("id")) : OptionalLong.empty();
            }
        }
    }

    /**
     * Moves a copy to a new status.
     *
     * <p>{@code expectedStatus} makes the update conditional: the row is only
     * written if it is still in the state the caller believed it was in. The
     * return value therefore reports whether this transaction actually made the
     * transition. Every caller checks it - a false result means the copy and the
     * rental disagree, and the transaction is rolled back rather than committing
     * a disc that is neither on the shelf nor accounted for.
     *
     * @return {@code true} if the row was updated
     */
    public boolean updateStatus(Connection connection, long copyId,
                                CopyStatus expectedStatus, CopyStatus newStatus) throws SQLException {
        String sql = "UPDATE copies SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus.name());
            statement.setLong(2, copyId);
            statement.setString(3, expectedStatus.name());
            return statement.executeUpdate() == 1;
        }
    }

    public List<Copy> findByMovie(Connection connection, long movieId) throws SQLException {
        String sql = "SELECT id, movie_id, barcode, status, acquired_at FROM copies "
                   + "WHERE movie_id = ? ORDER BY barcode";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.list(rs, MAPPER);
            }
        }
    }

    public int countAvailable(Connection connection, long movieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM copies WHERE movie_id = ? AND status = 'AVAILABLE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public Copy insert(Connection connection, Copy copy) throws SQLException {
        String sql = "INSERT INTO copies (movie_id, barcode, status) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, copy.getMovieId());
            statement.setString(2, copy.getBarcode());
            statement.setString(3, copy.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    copy.setId(keys.getLong(1));
                }
            }
        }
        return copy;
    }

    /**
     * @return the highest sequence number already issued in this title's
     *         barcodes, or {@code 0} if it has none yet
     *
     * <p>Reads the actual maximum rather than counting rows. A count would be
     * wrong the moment any copy row ceased to exist - five copies numbered 1-5
     * with number 3 removed would count 4 and reissue barcode 5, colliding with
     * the existing one under {@code uq_copies_barcode}.
     *
     * <p>Barcodes have the form {@code SRT-<movieId>-<sequence>}, so the
     * sequence is the segment after the last hyphen.
     */
    public int maxBarcodeSequence(Connection connection, long movieId) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(barcode, '-', -1) AS UNSIGNED)), 0)
                FROM   copies
                WHERE  movie_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
