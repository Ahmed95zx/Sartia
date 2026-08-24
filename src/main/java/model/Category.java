package model;

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

    /**
     * Version stamp used when Java turns an object of this class into bytes.
     *
     * <p>These objects are held in JSF view and session scope, and a servlet
     * container is allowed to serialise that state: to hand a session to
     * another server, or to keep it across a restart. Fixing the number by
     * hand means state written by an earlier build can still be read back
     * after a field is added, instead of failing on a version mismatch.
     */
    private static final long serialVersionUID = 1L;

    /** Primary key. Stays {@code 0} until the row has been inserted. */
    private long id;

    /** Display name shown in the sidebar and on the film page, e.g. "Comedy". */
    private String name;

    /** One line explaining the genre. Shown as a tooltip in the sidebar. */
    private String description;

    /** Not persisted - populated by the catalogue query for the sidebar counts. */
    private int movieCount;

    /** Empty constructor required by the JavaBean convention. */
    public Category() {
    }

    /** Convenience constructor used by the row mapper and by the tests. */
    public Category(long id, String name) {
        this.id = id;
        this.name = name;
    }

    /* ------------------------------------------------------------------
     * Accessors.
     *
     * JSF reads and writes these by name from the pages: an expression
     * such as #{movieBean.movie.title} calls getTitle(), and an input
     * bound to the same expression calls setTitle() when the form is
     * submitted. The DAO row mappers use them to fill an object from a
     * JDBC result set.
     *
     * They carry no logic of their own, so they are described here as a group
     * rather than repeating the same sentence above each one.
     * ------------------------------------------------------------------ */

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

    /**
     * Identity is the database id: two objects describe the same category when they
     * carry the same id.
     *
     * <p>The {@code id != 0} test is deliberate. An object built in memory but
     * not yet inserted has no id, and without that test every unsaved object
     * would compare equal to every other unsaved object. An unsaved object is
     * therefore equal only to itself.
     */
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

    /**
     * Built from the same field {@link #equals(Object)} compares.
     *
     * <p>Java requires this: two objects that are equal must return the same
     * hash code, otherwise looking one up in a {@code HashMap} or
     * {@code HashSet} quietly fails to find it.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Short form for log messages and debugging. Never shown to a customer. */
    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
