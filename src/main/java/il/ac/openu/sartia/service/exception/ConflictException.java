package il.ac.openu.sartia.service.exception;

/**
 * The request was well formed but conflicts with the system's current state -
 * the last copy was taken a moment ago, the username is now taken, the customer
 * is already at their borrowing limit.
 *
 * <p>Mapped to HTTP 409 by the REST layer. These are the outcomes that
 * concurrency produces, so the messages are written to tell the user what
 * changed rather than to imply they did something wrong.
 */
public class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
