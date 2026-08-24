package web;

import model.Rental;
import service.RentalService;
import service.exception.BusinessException;
import util.AppConfig;
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
 */
@Named("myRentalsBean")
@ViewScoped
public class MyRentalsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private RentalService rentalService;

    @Inject
    private SessionBean session;

    private List<Rental> open;
    private List<Rental> history;

    @PostConstruct
    public void init() {
        reload();
    }

    /** Returns a disc the customer is holding. */
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

    private void reload() {
        long userId = session.getUserId();
        open = rentalService.openRentalsFor(userId);
        history = rentalService.historyFor(userId);
    }

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

    public int getMaxConcurrent() {
        return AppConfig.maxConcurrentRentalsPerUser();
    }
}
