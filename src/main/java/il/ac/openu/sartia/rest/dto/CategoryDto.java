package il.ac.openu.sartia.rest.dto;

import il.ac.openu.sartia.model.Category;

/**
 * The public JSON shape of a category.
 *
 * @param id          category identifier
 * @param name        display name
 * @param description what the category covers
 * @param movieCount  how many titles it holds
 */
public record CategoryDto(long id, String name, String description, int movieCount) {

    public static CategoryDto from(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getMovieCount());
    }
}
