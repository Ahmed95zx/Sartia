package rest;

import dao.DataAccessException;
import service.exception.AuthenticationException;
import service.exception.BusinessException;
import service.exception.ConflictException;
import service.exception.NotFoundException;
import service.exception.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Translates exceptions thrown by the service layer into HTTP responses.
 *
 * <p>Without these, a rule violation would surface to a client as a 500 with a
 * Java stack trace in the body. Mapping them here keeps the services free of
 * any HTTP vocabulary while still producing accurate status codes.
 */
public final class ErrorMappers {

    private static final Logger LOG = Logger.getLogger(ErrorMappers.class.getName());

    private ErrorMappers() {
    }

    /**
     * The JSON body returned for every error, so clients can parse failures the
     * same way regardless of which one occurred.
     *
     * @param error   short machine-readable code
     * @param message human-readable explanation
     */
    public record ErrorDto(String error, String message) {
    }

    private static Response respond(Response.Status status, String code, String message) {
        return Response.status(status)
                .entity(new ErrorDto(code, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /** Domain rule violations, mapped to the status that describes each one. */
    @Provider
    public static class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

        @Override
        public Response toResponse(BusinessException failure) {
            if (failure instanceof NotFoundException) {
                return respond(Response.Status.NOT_FOUND, "not_found", failure.getMessage());
            }
            if (failure instanceof ValidationException) {
                return respond(Response.Status.BAD_REQUEST, "invalid_request", failure.getMessage());
            }
            if (failure instanceof ConflictException) {
                return respond(Response.Status.CONFLICT, "conflict", failure.getMessage());
            }
            if (failure instanceof AuthenticationException) {
                return respond(Response.Status.UNAUTHORIZED, "unauthorized", failure.getMessage());
            }
            return respond(Response.Status.BAD_REQUEST, "bad_request", failure.getMessage());
        }
    }

    /**
     * Infrastructure failures.
     *
     * <p>The real cause is logged but not returned: a database error message can
     * disclose schema details, and the client can do nothing with it either way.
     */
    @Provider
    public static class DataAccessExceptionMapper implements ExceptionMapper<DataAccessException> {

        @Override
        public Response toResponse(DataAccessException failure) {
            LOG.log(Level.SEVERE, "Data access failure serving REST request", failure);
            return respond(Response.Status.SERVICE_UNAVAILABLE, "storage_unavailable",
                    "The service is temporarily unavailable. Please try again.");
        }
    }
}
