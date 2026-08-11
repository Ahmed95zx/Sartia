package il.ac.openu.sartia.web;

import il.ac.openu.sartia.model.User;
import il.ac.openu.sartia.service.UserService;
import il.ac.openu.sartia.service.exception.BusinessException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/** Backs the login form. */
@Named("loginBean")
@RequestScoped
public class LoginBean {

    @Inject
    private UserService userService;

    @Inject
    private SessionBean session;

    private String username;
    private String password;

    /** Where to send the user after a successful login; set by the auth filter. */
    private String returnTo;

    /**
     * Verifies the credentials and starts the session.
     *
     * @return the outcome to navigate to, or {@code null} to redisplay the form
     *         with the error message
     */
    public String login() {
        try {
            User authenticated = userService.authenticate(username, password);
            session.login(authenticated);
            Messages.info("ברוך הבא, " + authenticated.getFullName());

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
