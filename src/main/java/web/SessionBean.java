package web;

import model.Role;
import model.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * Holds the identity of whoever is using this browser session.
 *
 * <p>Every page and every filter asks this bean who the user is, so it is the
 * single source of truth for authorisation. It stores the {@link User} with its
 * password hash already cleared by
 * {@link service.UserService#authenticate}.
 */
@Named("userSession")
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;

    /**
     * Records a successful login.
     *
     * <p>The session id is rotated first. An attacker who can plant a known
     * session id in a victim's browser before they sign in would otherwise
     * still hold a valid id afterwards - session fixation. Issuing a new id at
     * the moment privileges change closes that.
     *
     * <p>Rotation uses {@link HttpServletRequest#changeSessionId()} rather than
     * invalidating and recreating the session. Invalidating would also destroy
     * every {@code @SessionScoped} CDI bean attached to that session -
     * including this one - so the assignment below would be written to an
     * instance the container had already discarded, and the user would appear
     * never to have signed in. {@code changeSessionId} issues a new id while
     * keeping the session and its beans intact.
     */
    public void login(User authenticated) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // A session already exists here in practice, because JSF stores the
        // view state in one. getSession(true) makes that explicit rather than
        // relying on it, since changeSessionId() throws without a session.
        HttpSession session = request.getSession(true);
        if (session != null) {
            request.changeSessionId();
        }

        this.user = authenticated;
    }

    /** Ends the session and returns the customer to the catalogue. */
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/catalog.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public boolean isAdmin() {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** @return the signed-in user's id, or {@code 0} when nobody is signed in. */
    public long getUserId() {
        return user == null ? 0L : user.getId();
    }

    public String getDisplayName() {
        return user == null ? "" : user.getFullName();
    }
}
