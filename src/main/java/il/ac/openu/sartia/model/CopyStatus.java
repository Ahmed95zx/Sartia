package il.ac.openu.sartia.model;

/**
 * Lifecycle state of a single physical {@link Copy}.
 *
 * <p>Only {@link #AVAILABLE} copies are eligible to be rented; the rental
 * service filters on this value while holding a row lock, so the transition
 * AVAILABLE &rarr; RENTED is atomic with respect to competing customers.
 */
public enum CopyStatus {

    /** On the shelf, may be rented. */
    AVAILABLE("זמין"),

    /** Currently checked out to a customer. */
    RENTED("מושאל"),

    /** Written off - damaged or never returned. Never rentable again. */
    LOST("אבוד");

    private final String hebrewLabel;

    CopyStatus(String hebrewLabel) {
        this.hebrewLabel = hebrewLabel;
    }

    public String getHebrewLabel() {
        return hebrewLabel;
    }
}
