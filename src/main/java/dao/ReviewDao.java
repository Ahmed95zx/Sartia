package dao;

import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes {@code reviews}.
 *
 * <p>As in every DAO here, the {@link Connection} is supplied by the caller so
 * that the service layer owns the transaction.
 */
public class ReviewDao {

    /** Turns one row into a {@link Review}, author's name included. */
    private static final RowMapper<Review> MAPPER = rs -> {
        Review review = new Review();
        review.setId(rs.getLong("id"));
        review.setMovieId(rs.getLong("movie_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setRating(rs.getInt("rating"));
        review.setComment(rs.getString("comment"));
        review.setCreatedAt(RowMapper.dateTime(rs, "created_at"));
        review.setUserFullName(rs.getString("user_full_name"));
        return review;
    };

    /**
     * The shared head of both read queries, joined to {@code users} so each
     * review arrives with its author's name already attached.
     *
     * <p>Kept in one constant so the two methods below cannot drift apart, and
     * so the join is written once rather than copied.
     */
    private static final String SELECT_REVIEW = """
            SELECT r.id, r.movie_id, r.user_id, r.rating, r.comment, r.created_at,
                   u.full_name AS user_full_name
            FROM   reviews r
            JOIN   users u ON u.id = r.user_id
            """;

    /**
     * Every review of one title, newest first.
     *
     * <p>The id goes in through {@code setLong} on a placeholder rather than
     * being pasted into the SQL text. That is what makes SQL injection
     * impossible here: the driver sends the value separately from the
     * statement, so it can never be read as SQL.
     */
    public List<Review> findByMovie(Connection connection, long movieId) throws SQLException {
        String sql = SELECT_REVIEW + " WHERE r.movie_id = ? ORDER BY r.created_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.list(rs, MAPPER);
            }
        }
    }

    /**
     * One customer's review of one title, if they have written one.
     *
     * <p>Returns {@link Optional} rather than {@code null}, so the caller has to
     * face the "no review yet" case instead of discovering it as a
     * {@code NullPointerException}. The unique index on (movie, user)
     * guarantees there is at most one row to find.
     */
    public Optional<Review> findByMovieAndUser(Connection connection, long movieId, long userId) throws SQLException {
        String sql = SELECT_REVIEW + " WHERE r.movie_id = ? AND r.user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            statement.setLong(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.single(rs, MAPPER);
            }
        }
    }

    /**
     * Inserts a review, or overwrites the customer's existing one for the same
     * title.
     *
     * <p>{@code ON DUPLICATE KEY UPDATE} lets the database resolve this in one
     * statement against the unique index on {@code (movie_id, user_id)}. A
     * read-then-branch in Java would leave a window in which two concurrent
     * submissions both decide to insert, and one would then fail.
     */
    public void save(Connection connection, Review review) throws SQLException {
        String sql = """
                INSERT INTO reviews (movie_id, user_id, rating, comment)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE rating = VALUES(rating),
                                        comment = VALUES(comment),
                                        created_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, review.getMovieId());
            statement.setLong(2, review.getUserId());
            statement.setInt(3, review.getRating());
            statement.setString(4, review.getComment());
            statement.executeUpdate();
        }
    }
}
