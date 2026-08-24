package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A registered account - either a customer or an administrator.
 *
 * <p>The password hash is carried on the object because the DAO layer needs it
 * to verify a login, but it is never exposed to the presentation layer: the
 * REST resources project users through a DTO, and the JSF pages only ever read
 * {@link #getFullName()} and {@link #getRole()}.
 */
public class User implements Serializable {

    /**
     * Version stamp used when Java turns an object of this class into bytes.
     *
     * <p>These objects are held in JSF view and session scope, and a servlet
     * container is allowed to serialise that state: to hand a session to
     * another server, or to keep it across a restart. Fixing the number by
     * hand means state written by an earlier build can still be read back
     * after a field is added, instead of failing on a version mismatch.
     */
    private static final long serialVersionUID = 1L;

    /** Primary key. Stays {@code 0} until the row has been inserted. */
    private long id;

    /** Login name. A unique index on the table stops two people sharing one. */
    private String username;

    /**
     * The salted PBKDF2 hash of the password, never the password itself.
     *
     * <p>Storing only the hash means that even a full copy of the database
     * does not reveal what anybody typed. See {@code util.Passwords}.
     */
    private String passwordHash;

    /** Contact address. Also unique, so one address means one account. */
    private String email;

    /** Name greeted in the header and shown to staff on the returns screen. */
    private String fullName;

    /** Optional phone number. */
    private String phone;

    /** What this account is allowed to do. New accounts are customers. */
    private Role role = Role.CUSTOMER;

    /**
     * Whether the account may still sign in. Deactivating rather than deleting
     * keeps the rental history intact, which the foreign keys require anyway.
     */
    private boolean active = true;

    /** When the account was registered. Set by the database default. */
    private LocalDateTime createdAt;

    public User() {
        // Required by JavaBean conventions (JSF managed properties).
    }

    /** Convenience constructor for a new registration, before it has an id. */
    public User(String username, String email, String fullName) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    /** @return {@code true} if this account may manage the catalogue. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    /* ------------------------------------------------------------------
     * Accessors.
     *
     * JSF reads and writes these by name from the pages: an expression
     * such as #{movieBean.movie.title} calls getTitle(), and an input
     * bound to the same expression calls setTitle() when the form is
     * submitted. The DAO row mappers use them to fill an object from a
     * JDBC result set.
     *
     * They carry no logic of their own, so they are described here as a group
     * rather than repeating the same sentence above each one.
     * ------------------------------------------------------------------ */

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Identity is the database id: two objects describe the same account when they
     * carry the same id.
     *
     * <p>The {@code id != 0} test is deliberate. An object built in memory but
     * not yet inserted has no id, and without that test every unsaved object
     * would compare equal to every other unsaved object. An unsaved object is
     * therefore equal only to itself.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != 0 && id == user.id;
    }

    /**
     * Built from the same field {@link #equals(Object)} compares.
     *
     * <p>Java requires this: two objects that are equal must return the same
     * hash code, otherwise looking one up in a {@code HashMap} or
     * {@code HashSet} quietly fails to find it.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Short form for log messages and debugging. Never shown to a customer. */
    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}
