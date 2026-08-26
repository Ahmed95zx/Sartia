package sartia.persistence.dao;

import sartia.business.domain.Movie;
import sartia.business.domain.MovieSearchCriteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes {@code movies}, including the catalogue search.
 *
 * <p>Stock figures are computed by correlated sub-queries rather than kept in a
 * column on {@code movies}. A stored counter would need updating inside every
 * rental and return transaction and would be wrong the moment one of those
 * updates was missed; deriving it cannot drift.
 *
 * <h2>Why the keyword search uses LIKE and not a FULLTEXT index</h2>
 * A MySQL FULLTEXT index matches whole words or prefixes of them, so it cannot
 * match a fragment from the middle of a word: searching {@code batman} would
 * find nothing in "The Dark Knight", and a partial title such as {@code budap}
 * would never reach "The Grand Budapest Hotel". Substring matching has no such
 * blind spot, and against a catalogue of this size the full scan it costs is
 * not measurable. The index was removed rather than left in place unused.
 *
 * <p>The original Hebrew catalogue made this decision sharper still: Hebrew
 * attaches its definite article to the front of a word, so a prefix search for
 * {@code מטריקס} could never match {@code המטריקס} - the very search a customer
 * types. That was verified against the database before the index was dropped.
 * Were the catalogue to grow to tens of thousands of titles, the right answer
 * would be an external search engine, not a FULLTEXT index.
 */
public class MovieDao {

    /** Escape character for LIKE patterns, declared alongside every LIKE clause below. */
    private static final char LIKE_ESCAPE = '!';

    private static final RowMapper<Movie> MAPPER = rs -> {
        Movie movie = new Movie();
        movie.setId(rs.getLong("id"));
        movie.setTitle(rs.getString("title"));
        movie.setDescription(rs.getString("description"));
        movie.setDirector(rs.getString("director"));
        movie.setReleaseYear(RowMapper.nullableInt(rs, "release_year"));
        movie.setDurationMinutes(RowMapper.nullableInt(rs, "duration_min"));
        movie.setCategoryId(rs.getLong("category_id"));
        movie.setCategoryName(rs.getString("category_name"));
        movie.setCoverUrl(rs.getString("cover_url"));
        movie.setDailyPrice(rs.getBigDecimal("daily_price"));
        movie.setCreatedAt(RowMapper.dateTime(rs, "created_at"));
        movie.setTotalCopies(rs.getInt("total_copies"));
        movie.setAvailableCopies(rs.getInt("available_copies"));
        movie.setAverageRating(RowMapper.nullableDouble(rs, "avg_rating"));
        movie.setReviewCount(rs.getInt("review_count"));
        return movie;
    };

    private static final String SELECT_MOVIE = """
            SELECT m.id, m.title, m.description, m.director, m.release_year, m.duration_min,
                   m.category_id, m.cover_url, m.daily_price, m.created_at,
                   c.name AS category_name,
                   (SELECT COUNT(*) FROM copies cp
                     WHERE cp.movie_id = m.id)                          AS total_copies,
                   (SELECT COUNT(*) FROM copies cp
                     WHERE cp.movie_id = m.id AND cp.status = 'AVAILABLE') AS available_copies,
                   (SELECT AVG(r.rating) FROM reviews r
                     WHERE r.movie_id = m.id)                           AS avg_rating,
                   (SELECT COUNT(*) FROM reviews r
                     WHERE r.movie_id = m.id)                           AS review_count
            FROM   movies m
            JOIN   categories c ON c.id = m.category_id
            """;

    public Optional<Movie> findById(Connection connection, long id) throws SQLException {
        String sql = SELECT_MOVIE + " WHERE m.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.single(rs, MAPPER);
            }
        }
    }

    /**
     * Runs the catalogue search.
     *
     * <p>The {@code WHERE} clause is assembled from whichever criteria were
     * actually filled in. Only fixed SQL fragments are ever concatenated -
     * every user-supplied value goes in as a bind parameter, and the sort
     * column is chosen from an enum, so no part of the statement can be
     * influenced into changing the query's meaning.
     */
    public List<Movie> search(Connection connection, MovieSearchCriteria criteria) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String where = buildWhereClause(criteria, parameters);

        String sql = SELECT_MOVIE + where + orderByClause(criteria) + " LIMIT ? OFFSET ?";
        parameters.add(criteria.getPageSize());
        parameters.add(criteria.getOffset());

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.list(rs, MAPPER);
            }
        }
    }

    /** Total number of matches, so the UI can render page links. */
    public int countMatching(Connection connection, MovieSearchCriteria criteria) throws SQLException {
        List<Object> parameters = new ArrayList<>();
        String where = buildWhereClause(criteria, parameters);

        String sql = "SELECT COUNT(*) FROM movies m JOIN categories c ON c.id = m.category_id " + where;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Builds the WHERE clause for the supplied criteria, appending each bound
     * value to {@code parameters} in the same order the placeholders appear.
     */
    private String buildWhereClause(MovieSearchCriteria criteria, List<Object> parameters) {
        List<String> conditions = new ArrayList<>();

        if (criteria.hasKeyword()) {
            // Each word must appear somewhere in the title, director or synopsis;
            // the words themselves may be spread across those fields.
            for (String word : criteria.getKeyword().trim().split("\\s+")) {
                if (word.isEmpty()) {
                    continue;
                }
                conditions.add("(m.title LIKE ? ESCAPE '" + LIKE_ESCAPE + "'"
                             + " OR m.director LIKE ? ESCAPE '" + LIKE_ESCAPE + "'"
                             + " OR m.description LIKE ? ESCAPE '" + LIKE_ESCAPE + "')");
                String pattern = "%" + escapeLike(word) + "%";
                parameters.add(pattern);
                parameters.add(pattern);
                parameters.add(pattern);
            }
        }

        if (criteria.getCategoryId() != null) {
            conditions.add("m.category_id = ?");
            parameters.add(criteria.getCategoryId());
        }
        if (criteria.getYearFrom() != null) {
            conditions.add("m.release_year >= ?");
            parameters.add(criteria.getYearFrom());
        }
        if (criteria.getYearTo() != null) {
            conditions.add("m.release_year <= ?");
            parameters.add(criteria.getYearTo());
        }
        if (criteria.isOnlyAvailable()) {
            // EXISTS stops at the first available copy; COUNT(*) > 0 would scan them all.
            conditions.add("EXISTS (SELECT 1 FROM copies cp WHERE cp.movie_id = m.id AND cp.status = 'AVAILABLE')");
        }

        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    /** Maps the sort enum onto a fixed column. Nothing here comes from user input. */
    private String orderByClause(MovieSearchCriteria criteria) {
        String column = switch (criteria.getSortBy()) {
            case TITLE  -> "m.title";
            case YEAR   -> "m.release_year";
            case RATING -> "avg_rating";
            case NEWEST -> "m.created_at";
        };
        return " ORDER BY " + column + (criteria.isAscending() ? " ASC" : " DESC") + ", m.id ASC";
    }

    /** Neutralises LIKE wildcards so a literal % or _ in the term matches itself. */
    private String escapeLike(String term) {
        return term.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            statement.setObject(i + 1, parameters.get(i));
        }
    }

    public Movie insert(Connection connection, Movie movie) throws SQLException {
        String sql = """
                INSERT INTO movies (title, description, director, release_year, duration_min,
                                    category_id, cover_url, daily_price)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMovieFields(statement, movie);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    movie.setId(keys.getLong(1));
                }
            }
        }
        return movie;
    }

    public void update(Connection connection, Movie movie) throws SQLException {
        String sql = """
                UPDATE movies
                SET    title = ?, description = ?, director = ?, release_year = ?, duration_min = ?,
                       category_id = ?, cover_url = ?, daily_price = ?
                WHERE  id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindMovieFields(statement, movie);
            statement.setLong(9, movie.getId());
            statement.executeUpdate();
        }
    }

    private void bindMovieFields(PreparedStatement statement, Movie movie) throws SQLException {
        statement.setString(1, movie.getTitle());
        statement.setString(2, movie.getDescription());
        statement.setString(3, movie.getDirector());
        setNullableInt(statement, 4, movie.getReleaseYear());
        setNullableInt(statement, 5, movie.getDurationMinutes());
        statement.setLong(6, movie.getCategoryId());
        statement.setString(7, movie.getCoverUrl());
        statement.setBigDecimal(8, movie.getDailyPrice());
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    /**
     * Deletes a title. Copies cascade; the delete fails by foreign key if any
     * of those copies is referenced by a rental, which is the desired outcome -
     * rental history must survive catalogue edits.
     */
    public void delete(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM movies WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
