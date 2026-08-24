package dao;

import model.Role;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Reads and writes {@code users}.
 *
 * <p>Every method takes the {@link Connection} to use rather than opening its
 * own, so a service can enrol several DAO calls in one transaction.
 */
public class UserDao {

    private static final RowMapper<User> MAPPER = rs -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setActive(rs.getBoolean("active"));
        user.setCreatedAt(RowMapper.dateTime(rs, "created_at"));
        return user;
    };

    private static final String SELECT_COLUMNS =
            "SELECT id, username, password_hash, email, full_name, phone, role, active, created_at FROM users ";

    public Optional<User> findByUsername(Connection connection, String username) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                return RowMapper.single(rs, MAPPER);
            }
        }
    }

    /**
     * Takes an exclusive lock on one user's row, held until the surrounding
     * transaction commits.
     *
     * <p>This serialises a single customer's own concurrent requests. Rules
     * that are counted <em>per customer</em> - the borrowing limit, and the
     * refusal to lend a second copy of a title they already hold - are
     * otherwise read-then-act: two requests arriving together both read the
     * old state, both conclude they are within the rules, and both proceed.
     * Holding this lock makes the reads that follow it authoritative.
     *
     * <p>Different customers lock different rows, so this costs nothing in the
     * ordinary case of unrelated people renting at the same time.
     *
     * <p>Callers must take this lock <em>before</em> locking a copy. Every
     * transaction acquiring both locks in the same order (user, then copy) is
     * what keeps them from deadlocking against each other.
     *
     * @return {@code true} if the user exists and is now locked
     */
    public boolean lockUser(Connection connection, long userId) throws SQLException {
        String sql = "SELECT id FROM users WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * @return {@code true} if the username is already taken. Checked before
     *         insert to produce a friendly message; the unique index on the
     *         column remains the actual guarantee, since another request can
     *         claim the name between this check and the insert.
     */
    public boolean usernameExists(Connection connection, String username) throws SQLException {
        return existsBy(connection, "username", username);
    }

    public boolean emailExists(Connection connection, String email) throws SQLException {
        return existsBy(connection, "email", email);
    }

    private boolean existsBy(Connection connection, String column, String value) throws SQLException {
        // The column name is a compile-time constant supplied by this class,
        // never by user input, so concatenating it here cannot be injected into.
        String sql = "SELECT 1 FROM users WHERE " + column + " = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserts the user and populates its generated id. */
    public User insert(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, email, full_name, phone, role, active) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getFullName());
            statement.setString(5, user.getPhone());
            statement.setString(6, user.getRole().name());
            statement.setBoolean(7, user.isActive());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
        }
        return user;
    }
}
