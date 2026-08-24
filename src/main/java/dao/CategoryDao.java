package dao;

import model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Reads {@code categories}. Categories are seeded with the schema, so there is
 * no insert path.
 *
 * <p>A DAO (data access object) is the only kind of class in the project that
 * writes SQL. Keeping every statement behind this layer is what makes the
 * three-layer split real: the services above hold the rules and never mention a
 * table, and the pages above them never see a {@link java.sql.Connection}.
 *
 * <p>The connection is passed in rather than opened here. That is what lets the
 * service layer decide the transaction boundary and run several DAO calls
 * inside one, which is essential to how renting is made safe.
 */
public class CategoryDao {

    /**
     * Turns one row of a result set into a {@link Category}.
     *
     * <p>Written once and shared by the queries below, so a column added to the
     * table is read in one place rather than in each method separately.
     */
    private static final RowMapper<Category> MAPPER = rs -> {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        return category;
    };

    /**
     * Every category, alphabetically.
     *
     * <p>The try-with-resources brackets are not decoration: a statement and a
     * result set each hold a database resource, and this construct closes them
     * even if the query throws. Left unclosed under load they exhaust the
     * connection pool.
     */
    public List<Category> findAll(Connection connection) throws SQLException {
        String sql = "SELECT id, name, description FROM categories ORDER BY name";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return RowMapper.list(rs, MAPPER);
        }
    }

    /**
     * Categories together with how many titles each holds - used for the
     * catalogue sidebar. A LEFT JOIN keeps empty categories in the list with a
     * count of zero, which an inner join would silently drop.
     */
    public List<Category> findAllWithCounts(Connection connection) throws SQLException {
        String sql = """
                SELECT   c.id, c.name, c.description, COUNT(m.id) AS movie_count
                FROM     categories c
                LEFT JOIN movies m ON m.category_id = c.id
                GROUP BY c.id, c.name, c.description
                ORDER BY c.name
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return RowMapper.list(rs, row -> {
                Category category = MAPPER.map(row);
                category.setMovieCount(row.getInt("movie_count"));
                return category;
            });
        }
    }
}
