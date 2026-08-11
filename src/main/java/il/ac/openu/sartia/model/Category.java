package il.ac.openu.sartia.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A genre the catalogue is organised by (Action, Comedy, Drama, ...).
 *
 * <p>The specification requires the catalogue listing to be grouped by
 * predefined categories, so categories are seeded with the schema rather than
 * created ad hoc by customers.
 */
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String description;

    /** Not persisted - populated by the catalogue query for the sidebar counts. */
    private int movieCount;

    public Category() {
    }

    public Category(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMovieCount() {
        return movieCount;
    }

    public void setMovieCount(int movieCount) {
        this.movieCount = movieCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Category category)) {
            return false;
        }
        return id != 0 && id == category.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
