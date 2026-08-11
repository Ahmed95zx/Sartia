package il.ac.openu.sartia.dao;

import il.ac.openu.sartia.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Reads {@code categories}. Categories are seeded with the schema, so there is no insert path. */
public class CategoryDao {

    private static final RowMapper<Category> MAPPER = rs -> {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        return category;
    };

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
