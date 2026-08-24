package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for password hashing. No database required.
 *
 * <p>These are the security tests of the project. Each one states a property
 * that must hold for stored passwords to be worth anything, and would fail
 * loudly if the hashing were ever replaced with something weaker.
 *
 * <p>Written with JUnit 5. Three pieces of it appear throughout:
 * {@code @Test} marks one method as a test the framework should run;
 * {@code @DisplayName} gives that test the readable sentence that appears in
 * the report, which is why the method names can stay short; and the
 * {@code assert...} calls state what must be true, failing the test with the
 * expected and actual values when it is not.
 *
 * <p>Each test builds its own data and makes no assumption about what the
 * others did, so they can be run in any order or on their own.
 */
class PasswordsTest {

    @Test
    @DisplayName("a password verifies against its own hash")
    void verifiesCorrectPassword() {
        String hash = Passwords.hash("correct horse battery");
        assertTrue(Passwords.verify("correct horse battery", hash));
    }

    @Test
    @DisplayName("a wrong password is rejected")
    void rejectsWrongPassword() {
        String hash = Passwords.hash("correct horse battery");
        assertFalse(Passwords.verify("correct horse batteries", hash));
        assertFalse(Passwords.verify("", hash));
    }

    @Test
    @DisplayName("the same password hashes differently each time")
    void saltsEachHash() {
        // Equal hashes would mean no per-user salt, which is what makes a
        // precomputed rainbow table useless against this table.
        String first = Passwords.hash("same password");
        String second = Passwords.hash("same password");
        assertNotEquals(first, second);

        assertTrue(Passwords.verify("same password", first));
        assertTrue(Passwords.verify("same password", second));
    }

    /*
     * The cost factor is stored beside the hash rather than fixed in the code,
     * so it can be raised as machines get faster while hashes written under the
     * old setting still verify. This test pins both the format and a floor
     * under the cost.
     */
    @Test
    @DisplayName("the stored form records its iteration count")
    void encodesIterations() {
        String hash = Passwords.hash("whatever");
        String[] parts = hash.split(":");
        assertTrue(parts.length == 3, "expected iterations:salt:hash");
        assertTrue(Integer.parseInt(parts[0]) >= 100_000, "cost factor should be substantial");
    }

    @Test
    @DisplayName("malformed or missing stored hashes are rejected, not thrown on")
    void toleratesMalformedHash() {
        // A corrupt row must not take the login page down with an exception.
        assertFalse(Passwords.verify("password", "not-a-valid-hash"));
        assertFalse(Passwords.verify("password", "1:2"));
        assertFalse(Passwords.verify("password", "abc:!!!:!!!"));
        assertFalse(Passwords.verify("password", null));
        assertFalse(Passwords.verify(null, "210000:AAAA:BBBB"));
    }

    /*
     * The interface is Hebrew, so a Hebrew password is an ordinary case rather
     * than an exotic one. Hashing works on bytes, and getting the encoding
     * wrong would let a password verify on one machine and fail on another.
     */
    @Test
    @DisplayName("non-ASCII passwords round-trip")
    void handlesUnicode() {
        String hebrew = "סיסמה־חזקה־1234";
        String hash = Passwords.hash(hebrew);
        assertTrue(Passwords.verify(hebrew, hash));
        assertFalse(Passwords.verify("סיסמה־חזקה־1235", hash));
    }
}
