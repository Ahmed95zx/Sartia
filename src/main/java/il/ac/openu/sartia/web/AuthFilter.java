package il.ac.openu.sartia.web;

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

    @Inject
    private SessionBean session;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (!session.isLoggedIn()) {
            // Remember where they were headed so the login can send them back.
            response.sendRedirect(request.getContextPath()
                    + "/login.xhtml?returnTo=" + java.net.URLEncoder.encode(path, java.nio.charset.StandardCharsets.UTF_8));
            return;
        }

        boolean adminArea = !CUSTOMER_PAGES.contains(path);
        if (adminArea && !session.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "This area is for administrators only");
            return;
        }

        chain.doFilter(request, response);
    }
}
