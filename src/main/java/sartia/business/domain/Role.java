package sartia.business.domain;

/**
 * Authorisation level of a {@link User}.
 *
 * <p>Kept deliberately coarse: the specification distinguishes only between
 * customers, who rent titles, and administrators, who maintain the catalogue
 * and process returns.
 */
public enum Role {

    /** May browse, search, rent, return and review. */
    CUSTOMER,

    /** Everything a customer may do, plus catalogue and inventory management. */
    ADMIN
}
