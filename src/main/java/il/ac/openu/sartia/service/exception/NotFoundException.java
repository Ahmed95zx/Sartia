package il.ac.openu.sartia.service.exception;

/**
 * The requested entity does not exist.
 *
 * <p>Mapped to HTTP 404 by the REST layer. The JSF screens catch it where a
 * missing entity is an ordinary outcome - {@code MovieDetailBean.load} does so
 * to render its own "film not found" panel - rather than relying on a global
 * handler.
 */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException movie(long id) {
        return new NotFoundException("הסרט המבוקש (מזהה " + id + ") לא נמצא במערכת");
    }

    public static NotFoundException rental(long id) {
        return new NotFoundException("ההשאלה המבוקשת (מזהה " + id + ") לא נמצאה במערכת");
    }
}
