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

    private Messages() {
    }

    static void info(String text) {
        add(FacesMessage.SEVERITY_INFO, text);
    }

    static void error(String text) {
        add(FacesMessage.SEVERITY_ERROR, text);
    }

    private static void add(FacesMessage.Severity severity, String text) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(severity, text, null));
        // Without this the message is discarded by the redirect in the
        // post/redirect/get flow used after every successful form submission.
        context.getExternalContext().getFlash().setKeepMessages(true);
    }
}
