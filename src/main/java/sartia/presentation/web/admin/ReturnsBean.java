package sartia.presentation.web.admin;

import sartia.business.domain.Rental;
import sartia.business.service.RentalService;
import sartia.business.exception.BusinessException;
import sartia.presentation.web.SessionBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Backs the administrator's loans screen: everything currently out, which of it
 * is overdue, and the actions to take a disc back or write it off.
 *
 * <p>Being in the {@code sartia.presentation.web.admin} package grants nothing on its own. The
 * screen is reachable only because {@code AuthFilter} guards {@code /admin/*}
 * and refuses anyone without the ADMIN role; the package name merely groups
 * the code that serves those pages.
 *
 * <p>Note how close this class is to {@code MyRentalsBean}: both list loans and
 * both return discs. The difference is whose loans and on whose authority, and
 * that difference is settled in {@link RentalService}, not here.
 */
@Named("returnsBean")
@ViewScoped
public class ReturnsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Business layer: performs the return and the write-off. */
    @Inject
    private RentalService rentalService;

    /** The signed-in administrator, recorded as the person who acted. */
    @Inject
    private SessionBean session;

    /** Every disc currently out, across all customers. */
    private List<Rental> openRentals;

    /** The subset of those that are past their due date. */
    private List<Rental> overdue;

    /** Loads both lists once CDI has finished injecting the fields above. */
    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * Processes a return on the customer's behalf, at the counter.
     *
     * <p>The {@code true} argument is what separates this from the customer's
     * own return button: it tells the service that a member of staff is acting,
     * so the check that a customer may only return their own loan does not
     * apply. The administrator's id is passed as well, so the action is
     * attributable rather than anonymous.
     */
    public void processReturn(Rental rental) {
        try {
            BigDecimal lateFee = rentalService.returnRental(rental.getId(), session.getUserId(), true);
            if (lateFee.signum() > 0) {
                info("Returned: \"" + rental.getMovieTitle() + "\". Late fee: " + lateFee + " ₪");
            } else {
                info("Returned: \"" + rental.getMovieTitle() + "\"");
            }
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
        reload();
    }

    /**
     * Writes off a disc that will not be coming back.
     *
     * <p>This closes the loan and moves the copy to LOST, which takes it out of
     * stock permanently: no later search will offer it and no transaction will
     * lend it again. The title itself stays in the catalogue, since the shop may
     * still own other discs of it.
     */
    public void markLost(Rental rental) {
        try {
            rentalService.markCopyLost(rental.getId());
            info("Copy " + rental.getBarcode() + " was marked as lost");
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
        reload();
    }

    /** Re-reads both lists, so the screen reflects the database after an action. */
    private void reload() {
        openRentals = rentalService.allOpenRentals();
        overdue = rentalService.overdueRentals();
    }

    /*
     * These two put a message on the screen for the current request.
     *
     * They reach FacesContext directly instead of using the shared sartia.presentation.web.Messages
     * helper, which is an inconsistency worth noticing rather than defending:
     * both do the same thing, and one of them is one helper too many.
     */

    /** Adds a success notice to the page. */
    private void info(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, null));
    }

    /** Adds a failure notice to the page. */
    private void error(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }

    public List<Rental> getOpenRentals() {
        return openRentals;
    }

    /** Feeds the overdue badge in the screen heading. */
    public int getOverdueCount() {
        return overdue == null ? 0 : overdue.size();
    }
}
