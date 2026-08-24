package util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password hashing and verification.
 *
 * <h2>Why not a plain digest</h2>
 * SHA-256 of a password is designed to be fast, which is exactly wrong here: a
 * commodity GPU tries billions of candidates per second against a stolen table.
 * PBKDF2 deliberately costs {@value #ITERATIONS} HMAC rounds per guess, so the
 * same hardware manages a few hundred thousand.
 *
 * <h2>Why a per-user salt</h2>
 * A random salt per account means two customers who picked the same password
 * get different hashes, so a precomputed rainbow table is useless and each
 * account must be attacked individually.
 *
 * <p>Encoded form: {@code iterations:base64(salt):base64(hash)} - keeping the
 * iteration count inside the string means the cost can be raised later without
 * invalidating passwords already stored at the old cost.
 */
public final class Passwords {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Cost factor. Chosen so a single verification takes roughly 100 ms on the
     * development machine - slow enough to make offline cracking expensive,
     * fast enough that a login does not feel sluggish.
     */
    private static final int ITERATIONS = 210_000;

    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    /** @return the encoded hash to store in {@code users.password_hash} */
    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);

        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + ":" + encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash);
    }

    /** Convenience overload. Prefer the {@code char[]} form where the caller controls the buffer. */
    public static String hash(String password) {
        return hash(password.toCharArray());
    }

    /**
     * Verifies a candidate password against a stored hash.
     *
     * <p>The comparison uses {@link MessageDigest#isEqual}, which always
     * inspects every byte. A normal {@code equals} returns as soon as two bytes
     * differ, and that difference in timing is enough to recover a hash byte by
     * byte over many attempts.
     *
     * @return {@code true} if the password matches
     */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[1]);
            byte[] expected = decoder.decode(parts[2]);

            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException malformed) {
            // A stored hash we cannot parse is treated as no match rather than
            // as a crash - a corrupt row must not take the login page down.
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
            try {
                return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                // Clears the copy PBEKeySpec made of the password.
                spec.clearPassword();
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException failure) {
            throw new IllegalStateException("PBKDF2 is unavailable in this JVM", failure);
        }
    }

    /**
     * Command-line helper used to generate the hashes embedded in
     * {@code db/seed.sql}:
     * <pre>java util.Passwords admin123</pre>
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: Passwords <plaintext> [<plaintext> ...]");
            return;
        }
        for (String password : args) {
            System.out.println(password + " -> " + hash(password));
        }
    }
}
