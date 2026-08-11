package il.ac.openu.sartia.web.admin;

import il.ac.openu.sartia.model.Rental;
import il.ac.openu.sartia.service.RentalService;
import il.ac.openu.sartia.service.exception.BusinessException;
import il.ac.openu.sartia.web.SessionBean;
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
 */
@Named("returnsBean")
@ViewScoped
public class ReturnsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private RentalService rentalService;

    @Inject
    private SessionBean session;

    private List<Rental> openRentals;
    private List<Rental> overdue;

    @PostConstruct
    public void init() {
        reload();
    }

    /** Processes a return on the customer's behalf. */
    public void processReturn(Rental rental) {
        try {
            BigDecimal lateFee = rentalService.returnRental(rental.getId(), session.getUserId(), true);
            if (lateFee.signum() > 0) {
                info("הוחזר: \"" + rental.getMovieTitle() + "\". קנס איחור: " + lateFee + " ש\"ח");
            } else {
                info("הוחזר: \"" + rental.getMovieTitle() + "\"");
            }
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
        reload();
    }

    /** Writes off a disc that will not be coming back. */
    public void markLost(Rental rental) {
        try {
            rentalService.markCopyLost(rental.getId());
            info("העותק " + rental.getBarcode() + " סומן כאבוד");
        } catch (BusinessException failure) {
            error(failure.getMessage());
        }
        reload();
    }

    private void reload() {
        openRentals = rentalService.allOpenRentals();
        overdue = rentalService.overdueRentals();
    }

    private void info(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, text, null));
    }

    private void error(String text) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, text, null));
    }

    public List<Rental> getOpenRentals() {
        return openRentals;
    }

    public int getOverdueCount() {
        return overdue == null ? 0 : overdue.size();
    }
}
