package dao;

import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Reads and writes {@code reviews}. */
public class ReviewDao {

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

    private static final String SELECT_REVIEW = """
            SELECT r.id, r.movie_id, r.user_id, r.rating, r.comment, r.created_at,
                   u.full_name AS user_full_name
            FROM   reviews r
            JOIN   users u ON u.id = r.user_id
            """;

    public List<Review> findByMovie(Connection connection, long movieId) throws SQLException {
        String sql = SELECT_REVIEW + " WHERE r.movie_id = ? ORDER BY r.created_at DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movieId);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.list(rs, MAPPER);
            }
        }
    }

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
