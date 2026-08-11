package il.ac.openu.sartia.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Roots the JAX-RS layer at {@code /api}.
 *
 * <p>The REST layer is a second entry point onto the same services the JSF
 * pages use - it does not talk to the DAOs directly and carries no business
 * rules of its own. That is the point of the split: the lending rule that a
 * customer may hold three titles is enforced once, in
 * {@link il.ac.openu.sartia.service.RentalService}, no matter which entry point
 * the request arrived through.
 *
 * <p>Leaving the class body empty lets the runtime discover the resources and
 * providers by annotation scanning instead of maintaining a registry by hand.
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
}
