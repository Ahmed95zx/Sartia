package il.ac.openu.sartia.service;

import il.ac.openu.sartia.dao.Database;
import il.ac.openu.sartia.dao.UserDao;
import il.ac.openu.sartia.model.Role;
import il.ac.openu.sartia.model.User;
import il.ac.openu.sartia.service.exception.AuthenticationException;
import il.ac.openu.sartia.service.exception.ConflictException;
import il.ac.openu.sartia.service.exception.ValidationException;
import il.ac.openu.sartia.util.Passwords;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;
import java.util.regex.Pattern;

/** Registration, authentication and account administration. */
@ApplicationScoped
public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserDao userDao = new UserDao();

    /**
     * Creates a customer account.
     *
     * <p>The uniqueness pre-checks exist to produce a helpful message pointing
     * at the offending field. They are not the guarantee - two simultaneous
     * registrations can both pass them - so the unique-index violation is caught
     * as well and reported the same way.
     *
     * @throws ValidationException if any field is malformed
     * @throws ConflictException   if the username or email is taken
     */
    public User register(String username, String password, String email, String fullName, String phone) {
        validateRegistration(username, password, email, fullName);

        String passwordHash = Passwords.hash(password.toCharArray());

        return Database.inTransaction(connection -> {
            if (userDao.usernameExists(connection, username)) {
                throw new ConflictException("The username \"" + username + "\" is already taken");
            }
            if (userDao.emailExists(connection, email)) {
                throw new ConflictException("That email address is already registered");
            }

            User user = new User(username, email, fullName);
            user.setPasswordHash(passwordHash);
            user.setPhone(phone);
            user.setRole(Role.CUSTOMER);

            try {
                return userDao.insert(connection, user);
            } catch (SQLIntegrityConstraintViolationException duplicate) {
                throw new ConflictException("That username or email address is already registered");
            }
        });
    }

    /**
     * Verifies credentials.
     *
     * <p>When the username does not exist the password is still hashed against
     * a dummy value before failing. Returning immediately would make a missing
     * account measurably faster to reject than a wrong password, which is
     * enough to enumerate valid usernames from response times alone.
     *
     * @throws AuthenticationException if the credentials do not match or the account is disabled
     */
    public User authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw AuthenticationException.badCredentials();
        }

        Optional<User> found = Database.readOnly(
                connection -> userDao.findByUsername(connection, username.trim()));

        if (found.isEmpty()) {
            Passwords.verify(password, dummyHash());
            throw AuthenticationException.badCredentials();
        }

        User user = found.get();
        if (!Passwords.verify(password, user.getPasswordHash())) {
            throw AuthenticationException.badCredentials();
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account is disabled. Please contact an administrator.");
        }

        // The caller keeps this object in the HTTP session; the hash has no
        // business travelling with it.
        user.setPasswordHash(null);
        return user;
    }

    private void validateRegistration(String username, String password, String email, String fullName) {
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new ValidationException(
                    "The username must be 3-50 characters: letters, digits, dot, hyphen or underscore");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("The password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (email == null || !EMAIL.matcher(email).matches()) {
            throw new ValidationException("That email address is not valid");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("A full name is required");
        }
    }

    /**
     * A structurally valid hash of a value nobody knows, used only to spend the
     * same time verifying a nonexistent account as a real one.
     */
    private String dummyHash() {
        return "210000:AAAAAAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    }
}
