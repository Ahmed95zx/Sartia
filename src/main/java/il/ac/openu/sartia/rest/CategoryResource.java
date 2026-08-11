package il.ac.openu.sartia.rest;

import il.ac.openu.sartia.rest.dto.CategoryDto;
import il.ac.openu.sartia.service.CatalogService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** Read-only JSON access to the category list. */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject
    private CatalogService catalogService;

    /** Every category with the number of titles it holds. */
    @GET
    public List<CategoryDto> all() {
        return catalogService.categoriesWithCounts().stream().map(CategoryDto::from).toList();
    }
}
