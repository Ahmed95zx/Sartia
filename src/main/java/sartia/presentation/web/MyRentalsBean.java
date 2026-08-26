package sartia.presentation.web;

import sartia.business.domain.Rental;
import sartia.business.service.RentalService;
import sartia.business.exception.BusinessException;
import sartia.common.config.AppConfig;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Backs "my rentals": what the customer currently holds, and their full
 * borrowing history.
 *
 * <p>This class belongs to the presentation layer. It holds no rules of its
 * own: it asks {@link RentalService} for data, hands it to the page, and turns
 * whatever comes back into a message. Every decision about whether an action
 * is allowed is taken in the business layer, so the same rules apply to the
 * REST callers that never touch this class.
 *
 * <p><b>{@code @Named}</b> publishes the bean to the pages under the name in
 * brackets, which is how {@code #{myRentalsBean.open}} in my-rentals.xhtml
 * finds it.
 *
 * <p><b>{@code @ViewScoped}</b> means one instance lives as long as the
 * customer stays on this page, surviving the form submissions made on it. That
 * is what allows the lists below to be loaded once rather than on every click.
 * A request-scoped bean would be rebuilt for each button press, and a
 * session-scoped one would still be holding this page's data after the
 * customer had navigated away. View scope requires {@link Serializable},
 * because JSF may write the view state out between requests.
 */
@Named("myRentalsBean")
@ViewScoped
public class MyRentalsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The business layer this screen talks to.
     *
     * <p>{@code @Inject} asks CDI to supply the object rather than writing
     * {@code new RentalService()} here. The bean therefore does not decide how
     * the service is built or how long it lives, which is what lets the tests
     * exercise the service on its own.
     */
    @Inject
    private RentalService rentalService;

    /** Who is signed in. Supplies the customer id for every query below. */
    @Inject
    private SessionBean session;

    /** Discs the customer is holding right now, each with a return button. */
    private List<Rental> open;

    /** Every past loan, returned ones included, newest first. */
    private List<Rental> history;

    /**
     * Loads the two lists once the bean is ready.
     *
     * <p>This work cannot go in a constructor: CDI creates the object first and
     * fills the {@code @Inject} fields afterwards, so {@code rentalService}
     * would still be {@code null}. {@code @PostConstruct} marks a method to run
     * after injection has finished, which is the first safe moment.
     */
    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Returns a disc the customer is holding.
     *
     * <p>Called from the return button on my-rentals.xhtml, which passes the
     * rental id as a parameter. The customer id is taken from the session and
     * not from the page, so altering the submitted form cannot return somebody
     * else's loan; the service checks the two against each other. The
     * {@code false} argument says this is an ordinary return rather than a
     * disc being written off as lost, which only staff may do.
     *
     * <p>Both lists are reloaded afterwards, whether the return succeeded or
     * failed, so the screen always reflects what is actually in the database.
     */
    public void returnRental(long rentalId) {
        try {
            BigDecimal lateFee = rentalService.returnRental(rentalId, session.getUserId(), false);
            if (lateFee.signum() > 0) {
                Messages.info("Film returned. A late fee of " + lateFee + " ₪ was charged.");
            } else {
                Messages.info("Film returned successfully. Thank you!");
            }
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
        }
        reload();
    }

    /** Re-reads both lists for the signed-in customer. */
    private void reload() {
        long userId = session.getUserId();
        open = rentalService.openRentalsFor(userId);
        history = rentalService.historyFor(userId);
    }

    /* Read by the page through #{myRentalsBean...}. */

    public List<Rental> getOpen() {
        return open;
    }

    public List<Rental> getHistory() {
        return history;
    }

    /** How many more titles this customer may take out right now. */
    public int getRemainingAllowance() {
        return Math.max(0, AppConfig.maxConcurrentRentalsPerUser() - (open == null ? 0 : open.size()));
    }

    /** The borrowing limit itself, so the page can explain the number it shows. */
    public int getMaxConcurrent() {
        return AppConfig.maxConcurrentRentalsPerUser();
    }
}
