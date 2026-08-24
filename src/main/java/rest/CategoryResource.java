package rest;

import rest.dto.CategoryDto;
import service.CatalogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Read-only JSON access to the category list.
 *
 * <p>Part of the REST interface, which is the third of the course technologies
 * this project uses alongside JSF and JDBC. It exposes the same catalogue that
 * the web pages show, but as data rather than as HTML, so another program could
 * consume it.
 *
 * <p><b>{@code @Path}</b> fixes the URL this class answers on. Combined with
 * the {@code /api} prefix declared in {@link RestApplication}, and the context
 * root the application is deployed under, the full address is
 * {@code /sartia/api/categories}.
 *
 * <p><b>{@code @Produces}</b> declares that the replies are JSON. The framework
 * converts the returned objects for us; nothing here writes JSON by hand.
 *
 * <p>There is no write path on purpose. Categories are fixed by the schema, and
 * a read-only resource cannot be used to alter anything.
 */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    /**
     * The same business layer the JSF beans use.
     *
     * <p>This is the point of layering: the REST interface is another caller in
     * front of the identical rules, not a second copy of them.
     */
    @Inject
    private CatalogService catalogService;

    /**
     * Every category with the number of titles it holds.
     *
     * <p>{@code @GET} maps this method to an HTTP GET on the class path above.
     *
     * <p>The result is converted to {@link CategoryDto} rather than returned as
     * the model objects themselves. That keeps the published JSON a deliberate
     * choice: internal fields are not exposed to callers by accident, and
     * renaming a field in the model does not silently change the API.
     */
    @GET
    public List<CategoryDto> all() {
        return catalogService.categoriesWithCounts().stream().map(CategoryDto::from).toList();
    }
}
