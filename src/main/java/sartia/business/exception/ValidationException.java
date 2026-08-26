package sartia.business.exception;

/**
 * The caller supplied input that the domain rejects - a blank title, a rating
 * outside 1-5, a password that is too short.
 *
 * <p>Mapped to HTTP 400 by the REST layer, and shown by the JSF screens as a
 * message at the top of the page. Field-level messages come from JSF's own
 * validators (a required field, a malformed number); this exception carries the
 * rules the domain enforces, which often span several fields at once.
 */
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
