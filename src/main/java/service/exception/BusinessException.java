package service.exception;

/**
 * A rule of the business domain was broken.
 *
 * <p>Distinct from {@link dao.DataAccessException}, which
 * signals that the system itself malfunctioned. A business exception is an
 * expected outcome - "that title is out of stock" - and its message is written
 * to be shown to the user directly, in Hebrew, without translation by the
 * presentation layer.
 */
public abstract class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected BusinessException(String message) {
        super(message);
    }
}
