package rest;

import model.MovieSearchCriteria;
import rest.dto.MovieDto;
import rest.dto.PageDto;
import rest.dto.ReviewDto;
import service.CatalogService;
import service.ReviewService;
import service.exception.ValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Read-only JSON access to the catalogue.
 *
 * <p>Kept to safe methods on purpose. Renting changes stock and must be tied to
 * an authenticated customer; exposing it here would mean building a second
 * authentication mechanism alongside the servlet session the JSF screens use,
 * and getting one of them subtly wrong. Reads carry no such risk, and they are
 * what a companion client - a mobile app, or the catalogue page's own
 * type-ahead - actually needs.
 */
@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    /** Bounded so a client cannot ask for the entire catalogue in one response. */
    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    private CatalogService catalogService;

    @Inject
    private ReviewService reviewService;

    /**
     * Searches the catalogue.
     *
     * <pre>
     * GET /api/movies?q=matrix&amp;categoryId=4&amp;available=true&amp;page=1&amp;size=12
     * </pre>
     *
     * @param keyword    free-text term matched against title, synopsis and director
     * @param categoryId restrict to one category
     * @param available  when true, only titles with a copy on the shelf
     * @param page       one-based page number
     * @param size       results per page, capped at {@value #MAX_PAGE_SIZE}
     */
    @GET
    public PageDto<MovieDto> search(
            @QueryParam("q") String keyword,
            @QueryParam("categoryId") Long categoryId,
            @QueryParam("available") @DefaultValue("false") boolean available,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("12") int size) {

        if (page < 1) {
            throw new ValidationException("page must be 1 or greater");
        }
        if (size < 1) {
            throw new ValidationException("size must be 1 or greater");
        }

        MovieSearchCriteria criteria = new MovieSearchCriteria();
        criteria.setKeyword(keyword);
        criteria.setCategoryId(categoryId);
        criteria.setOnlyAvailable(available);
        criteria.setPage(page);
        criteria.setPageSize(Math.min(size, MAX_PAGE_SIZE));

        CatalogService.SearchResult result = catalogService.search(criteria);

        List<MovieDto> items = result.getMovies().stream().map(MovieDto::from).toList();
        return PageDto.of(items, result.getPage(), result.getPageSize(), result.getTotalCount());
    }

    /**
     * One title.
     *
     * @throws service.exception.NotFoundException mapped to 404
     */
    @GET
    @Path("/{id}")
    public MovieDto byId(@PathParam("id") long id) {
        return MovieDto.from(catalogService.findMovie(id));
    }

    /** Reviews of one title, newest first. */
    @GET
    @Path("/{id}/reviews")
    public List<ReviewDto> reviews(@PathParam("id") long id) {
        // Resolve the title first so an unknown id answers 404 rather than an
        // empty list, which would wrongly suggest the film exists but has no reviews.
        catalogService.findMovie(id);
        return reviewService.forMovie(id).stream().map(ReviewDto::from).toList();
    }
}
