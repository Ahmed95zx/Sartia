package sartia.presentation.security;

import sartia.presentation.web.SessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Guards the pages that require a signed-in user, and the administration area
 * that additionally requires the ADMIN role.
 *
 * <p>Enforcement lives in a filter rather than in each page because a check
 * that has to be remembered on every new screen is a check that will eventually
 * be forgotten. Here a page is protected by virtue of its URL, and the rendered
 * navigation merely reflects that rule - it is not what enforces it.
 */
@WebFilter(urlPatterns = { "/my-rentals.xhtml", "/admin/*" })
public class AuthFilter implements Filter {

    /** Pages any signed-in user may reach. */
    private static final Set<String> CUSTOMER_PAGES = Set.of("/my-rentals.xhtml");

    /** Who is signed in, if anyone. The same session bean the pages read. */
    @Inject
    private SessionBean session;

    /**
     * Runs before every request whose URL matches the patterns above.
     *
     * <p>A servlet filter sits in front of the application and decides whether
     * a request reaches it. Calling {@code chain.doFilter} passes the request
     * on to the page; returning without calling it stops the request here, and
     * the redirect or error already written is what the browser receives.
     *
     * <p>The two refusals are deliberately different. A visitor who is not
     * signed in has something to do about it, so they are redirected to the
     * login form with their destination remembered. A signed-in customer who
     * asks for the admin area has nothing to do about it, so they get 403
     * rather than a login form that would not help them.
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // The URI includes the context root the application is deployed
        // under (/sartia). Stripping it leaves the path as the patterns above
        // and CUSTOMER_PAGES below write it, so the rules do not depend on
        // where the application happens to be deployed.
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (!session.isLoggedIn()) {
            // Remember where they were headed so the login can send them back.
            // The path is percent-encoded because it becomes the value of a
            // query parameter, and an unencoded "/" or "?" would be read as
            // part of the login URL rather than as data.
            response.sendRedirect(request.getContextPath()
                    + "/login.xhtml?returnTo=" + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        // Anything this filter guards that is not on the customer list is,
        // by definition, in the admin area. Stating it this way round means a
        // new admin page is protected the moment it exists, whereas a list of
        // admin paths would leave a new page unguarded until somebody
        // remembered to add it.
        boolean adminArea = !CUSTOMER_PAGES.contains(path);
        if (adminArea && !session.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This area is for administrators only");
            return;
        }

        chain.doFilter(request, response);
    }
}
