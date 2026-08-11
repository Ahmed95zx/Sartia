package il.ac.openu.sartia.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the overdue and late-fee rules. No database required. */
class RentalTest {

    private static Rental open(LocalDate dueDate) {
        Rental rental = new Rental();
        rental.setRentedAt(LocalDateTime.now().minusDays(3));
        rental.setDueDate(dueDate);
        return rental;
    }

    /**
     * Compares amounts by value. {@link BigDecimal#equals} also compares scale,
     * so {@code 0} and {@code 0.00} are unequal to it - a distinction these
     * tests are not making assertions about.
     */
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    @DisplayName("a rental with no return timestamp is open")
    void openWhileNotReturned() {
        assertTrue(open(LocalDate.now().plusDays(2)).isOpen());
    }

    @Test
    @DisplayName("a returned rental is closed and never reported overdue")
    void returnedIsNotOverdue() {
        Rental rental = open(LocalDate.now().minusDays(10));
        rental.setReturnedAt(LocalDateTime.now());
        rental.setLateFee(new BigDecimal("9.00"));

        assertFalse(rental.isOpen());
        // Returned late is represented by the fee that was charged, not by
        // continuing to report the rental as overdue forever.
        assertFalse(rental.isOverdue());
        assertAmount("9.00", rental.getProjectedLateFee());
    }

    @Test
    @DisplayName("due today is not yet overdue")
    void dueTodayIsNotOverdue() {
        Rental rental = open(LocalDate.now());
        assertFalse(rental.isOverdue());
        assertEquals(0, rental.getDaysOverdue());
        assertAmount("0", rental.getProjectedLateFee());
    }

    @Test
    @DisplayName("one day past due accrues one day of fee")
    void oneDayOverdue() {
        Rental rental = open(LocalDate.now().minusDays(1));
        assertTrue(rental.isOverdue());
        assertEquals(1, rental.getDaysOverdue());
        assertAmount("3.00", rental.getProjectedLateFee());
    }

    @Test
    @DisplayName("the fee scales with the number of days late")
    void feeScalesWithDelay() {
        Rental rental = open(LocalDate.now().minusDays(4));
        assertEquals(4, rental.getDaysOverdue());
        assertAmount("12.00", rental.getProjectedLateFee());
    }

    @Test
    @DisplayName("a rental well within its period owes nothing")
    void withinPeriodOwesNothing() {
        Rental rental = open(LocalDate.now().plusDays(5));
        assertFalse(rental.isOverdue());
        assertAmount("0", rental.getProjectedLateFee());
    }
}
