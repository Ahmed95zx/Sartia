package sartia.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Read-only access to the business rules in {@code sartia.properties}.
 *
 * <p>The lending period and the borrowing limit are policy, not logic: a branch
 * manager may want to change them without a rebuild. Keeping them here rather
 * than as literals scattered through the service layer means there is exactly
 * one place to change, and the design document has one place to cite.
 */
public final class AppConfig {

    /**
     * The file's contents, read once when the class is first used.
     *
     * <p>Java loads a class lazily and initialises its static fields exactly
     * once, so this reads the file a single time for the whole application
     * without any locking of our own.
     */
    private static final Properties PROPERTIES = load();

    /** Utility class: never instantiated, hence the private constructor. */
    private AppConfig() {
    }

    /** Days a customer may keep a disc before it is overdue. */
    public static int rentalPeriodDays() {
        return intValue("rental.periodDays", 7);
    }

    /** Maximum number of discs one customer may hold simultaneously. */
    public static int maxConcurrentRentalsPerUser() {
        return intValue("rental.maxConcurrentPerUser", 3);
    }

    /** Charged for each day a rental is past its due date. */
    public static java.math.BigDecimal lateFeePerDay() {
        String raw = System.getProperty("rental.lateFeePerDay",
                                        PROPERTIES.getProperty("rental.lateFeePerDay"));
        if (raw == null || raw.isBlank()) {
            return new java.math.BigDecimal("3.00");
        }
        try {
            return new java.math.BigDecimal(raw.trim());
        } catch (NumberFormatException malformed) {
            return new java.math.BigDecimal("3.00");
        }
    }

    /**
     * Reads a whole-number setting.
     *
     * <p>A JVM system property is consulted before the file, which is what lets
     * the same WAR be started against different settings without rebuilding:
     * {@code java -Drental.periodDays=14 ...} overrides the file.
     *
     * <p>Every failure path ends at the supplied default: a missing key, a blank
     * value, or text that is not a number. A typo in a configuration file should
     * make the shop use its normal lending period, not refuse to open.
     */
    private static int intValue(String key, int fallback) {
        String raw = System.getProperty(key, PROPERTIES.getProperty(key));
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException malformed) {
            // A typo in the configuration file must not stop the application
            // from starting; fall back to the documented default.
            return fallback;
        }
    }

    /**
     * Reads {@code sartia.properties} from the classpath.
     *
     * <p>Loaded through the class loader rather than as a file path, because
     * inside a deployed WAR the file is an entry in an archive and has no path
     * on disk to open.
     *
     * <p>A missing file is tolerated - every getter above has a default - but a
     * file that exists and cannot be read is not, since that points at a real
     * problem worth failing loudly for.
     */
    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("sartia.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not read sartia.properties", failure);
        }
        return properties;
    }
}
