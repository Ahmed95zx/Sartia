package il.ac.openu.sartia.service.exception;

/**
 * Login failed, or an action was attempted without the necessary rights.
 *
 * <p>The message is deliberately vague about <em>why</em> a login failed: telling
 * an attacker that the username exists but the password was wrong turns a login
 * form into a tool for enumerating accounts.
 */
public class AuthenticationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public static AuthenticationException badCredentials() {
        return new AuthenticationException("שם משתמש או סיסמה שגויים");
    }
}
