package web;

import model.User;
import service.UserService;
import service.exception.BusinessException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Backs the login form.
 *
 * <p><b>{@code @RequestScoped}</b>, unlike the view-scoped beans: a fresh
 * instance is created for the login attempt and thrown away as soon as the
 * response has been written. Credentials therefore live in memory for the
 * shortest time the framework allows. What survives the login is held by
 * {@link SessionBean}, which is a different object with a different lifetime.
 */
@Named("loginBean")
@RequestScoped
public class LoginBean {

    /** Business layer: verifies the credentials. */
    @Inject
    private UserService userService;

    /** Where the signed-in user is recorded once the check has passed. */
    @Inject
    private SessionBean session;

    /** Login name as typed into the form. */
    private String username;

    /** Password as typed. Cleared in the {@code finally} block below. */
    private String password;

    /** Where to send the user after a successful login; set by the auth filter. */
    private String returnTo;

    /**
     * Verifies the credentials and starts the session.
     *
     * <p>A JSF action method returns the next view as a string. Two details of
     * that convention are worth spelling out:
     * <ul>
     *   <li>Returning {@code null} tells JSF to stay on the current view and
     *       render it again. That is how a failed login redisplays the form
     *       with the error message attached, without losing what was typed.</li>
     *   <li>{@code faces-redirect=true} turns the outcome into a real HTTP
     *       redirect instead of forwarding internally. The browser then issues
     *       a fresh GET for the new page, so its address bar matches what is
     *       on screen and a later refresh does not re-submit the login form.
     *       This is the post-redirect-get pattern.</li>
     * </ul>
     *
     * <p>Where the user lands depends on why they arrived: back to the page the
     * filter intercepted if there was one, otherwise the management screen for
     * an administrator and the catalogue for a customer.
     *
     * @return the outcome to navigate to, or {@code null} to redisplay the form
     *         with the error message
     */
    public String login() {
        try {
            User authenticated = userService.authenticate(username, password);
            session.login(authenticated);
            Messages.info("Welcome, " + authenticated.getFullName());

            if (returnTo != null && !returnTo.isBlank()) {
                return returnTo + (returnTo.contains("?") ? "&" : "?") + "faces-redirect=true";
            }
            return authenticated.isAdmin()
                    ? "/admin/movies.xhtml?faces-redirect=true"
                    : "/catalog.xhtml?faces-redirect=true";
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
            return null;
        } finally {
            // Never leave the submitted password in a bean the container may
            // keep alive longer than this request.
            password = null;
        }
    }

    /* Bound to the form inputs on login.xhtml. */

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

    public String getReturnTo() {
        return returnTo;
    }

    public void setReturnTo(String returnTo) {
        this.returnTo = returnTo;
    }
}
