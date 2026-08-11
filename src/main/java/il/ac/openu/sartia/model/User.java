package il.ac.openu.sartia.model;

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

    private static final long serialVersionUID = 1L;

    private long id;
    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private String phone;
    private Role role = Role.CUSTOMER;
    private boolean active = true;
    private LocalDateTime createdAt;

    public User() {
        // Required by JavaBean conventions (JSF managed properties).
    }

    public User(String username, String email, String fullName) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
    }

    /** @return {@code true} if this account may manage the catalogue. */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + '}';
    }
}
