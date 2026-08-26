package sartia.presentation.web;

import sartia.business.domain.Role;
import sartia.business.domain.User;
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
 * {@link sartia.business.service.UserService#authenticate}.
 */
@Named("userSession")
@SessionScoped
public class SessionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The signed-in account, or {@code null} when nobody is signed in.
     *
     * <p>{@code @SessionScoped} means one instance of this bean per browser
     * session, so this field is what makes a login persist from one request to
     * the next. It is the only mutable state the application keeps between
     * requests; everything else is read from the database each time.
     */
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

    /**
     * Ends the session and returns the customer to the catalogue.
     *
     * <p>Invalidating is right here, and wrong in {@link #login(User)}. On the
     * way out, destroying every session-scoped bean is exactly the intent: no
     * fragment of the previous user should survive for whoever uses the browser
     * next.
     */
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/catalog.xhtml?faces-redirect=true";
    }

    /** @return whether anybody is signed in on this session. */
    public boolean isLoggedIn() {
        return user != null;
    }

    /**
     * @return whether the signed-in account may manage the catalogue.
     *
     * <p>Read both by {@code AuthFilter}, where it decides access, and by the
     * pages, where it only decides what to draw.
     */
    public boolean isAdmin() {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** @return the signed-in user's id, or {@code 0} when nobody is signed in. */
    public long getUserId() {
        return user == null ? 0L : user.getId();
    }

    /** @return the name to greet in the header, or empty when signed out. */
    public String getDisplayName() {
        return user == null ? "" : user.getFullName();
    }
}
