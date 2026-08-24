package web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

/**
 * Adds user-facing messages to the current JSF response.
 *
 * <p>Wrapping the {@link FacesContext} calls keeps the backing beans readable
 * and puts the "survive the redirect" decision in one place: messages are
 * marked to persist across the redirect that follows every successful POST, so
 * the confirmation is still on screen after the browser follows it.
 */
final class Messages {

    /** Utility class: never instantiated, hence the private constructor. */
    private Messages() {
    }

    /** Shows a green confirmation on the next page the user sees. */
    static void info(String text) {
        add(FacesMessage.SEVERITY_INFO, text);
    }

    /** Shows a red failure notice on the next page the user sees. */
    static void error(String text) {
        add(FacesMessage.SEVERITY_ERROR, text);
    }

    /**
     * Queues one message on the current response.
     *
     * <p>The {@code null} first argument attaches it to the page as a whole
     * rather than to a named input, which is what the message panel in the
     * shared layout renders.
     */
    private static void add(FacesMessage.Severity severity, String text) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, text, null));
        // Without this the message is discarded by the redirect in the
        // post/redirect/get flow used after every successful form submission.
        context.getExternalContext().getFlash().setKeepMessages(true);
    }
}
