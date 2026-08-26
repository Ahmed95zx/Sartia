package sartia.presentation.web;

import sartia.business.domain.User;
import sartia.business.service.UserService;
import sartia.business.exception.BusinessException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Backs the customer registration form.
 *
 * <p>{@code @RequestScoped} for the same reason as {@link LoginBean}: the
 * chosen password passes through this object, so it should live no longer than
 * the one request that carries it.
 *
 * <p>Only one check happens here, that the two password boxes agree. That is a
 * property of this form rather than a rule about accounts: the REST layer has
 * no second box to compare. Every real rule, such as whether the name is taken
 * or the address is well formed, is enforced by {@link UserService} where all
 * callers meet it.
 */
@Named("registerBean")
@RequestScoped
public class RegisterBean {

    /** Business layer: applies the account rules and stores the new user. */
    @Inject
    private UserService userService;

    /** Where the new customer is recorded as signed in. */
    @Inject
    private SessionBean session;

    /** Chosen login name. Must not already be taken. */
    private String username;

    /** Chosen password. Cleared in the {@code finally} block below. */
    private String password;

    /** Second copy of the password, compared with the first before anything else. */
    private String passwordConfirm;

    /** Contact address. Must be unique across accounts. */
    private String email;

    /** Name used to greet the customer once signed in. */
    private String fullName;

    /** Optional phone number. */
    private String phone;

    /**
     * Creates the account and signs the new customer straight in - asking
     * someone to type the credentials they just chose adds nothing.
     *
     * <p>Returning {@code null} from either failure path keeps the customer on
     * the form with their other answers intact and the reason shown. On success
     * the outcome carries {@code faces-redirect=true}, so a refresh of the
     * catalogue afterwards cannot re-submit the registration.
     *
     * <p>The {@code finally} block clears both password fields whichever way
     * the method leaves, so the plain text is not still sitting in the object
     * while the container finishes with it.
     *
     * @return the catalogue on success, or {@code null} to redisplay the form
     */
    public String register() {
        if (password == null || !password.equals(passwordConfirm)) {
            Messages.error("The passwords do not match");
            return null;
        }

        try {
            User created = userService.register(username, password, email, fullName, phone);
            session.login(created);
            Messages.info("Registration complete. Welcome, " + created.getFullName());
            return "/catalog.xhtml?faces-redirect=true";
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
            return null;
        } finally {
            password = null;
            passwordConfirm = null;
        }
    }

    /* Bound to the form inputs on register.xhtml. */

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
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
}
