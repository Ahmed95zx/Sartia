package sartia.business.domain;

/**
 * Lifecycle state of a single physical {@link Copy}.
 *
 * <p>Only {@link #AVAILABLE} copies are eligible to be rented; the rental
 * service filters on this value while holding a row lock, so the transition
 * AVAILABLE &rarr; RENTED is atomic with respect to competing customers.
 */
public enum CopyStatus {

    /** On the shelf, may be rented. */
    AVAILABLE("Available"),

    /** Currently checked out to a customer. */
    RENTED("Rented"),

    /** Written off - damaged or never returned. Never rentable again. */
    LOST("Lost");

    private final String label;

    CopyStatus(String label) {
        this.label = label;
    }

    /** Display text for the status, used by the inventory screen. */
    public String getLabel() {
        return label;
    }
}
