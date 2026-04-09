package co.razkevich.sflocalstack.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates valid 18-character Salesforce IDs.
 *
 * Format: [3-char key prefix] + [12-char unique body] + [3-char case-fold suffix]
 *
 * The 3-char suffix is computed using the standard Salesforce algorithm:
 * the 15-char base is split into three 5-char chunks; for each chunk a 5-bit
 * integer is formed where bit N is 1 if character N is uppercase; each integer
 * maps to a character in "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345".
 */
public final class SalesforceIdGenerator {

    private static final String SUFFIX_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ012345";
    private static final String BODY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final AtomicLong COUNTER = new AtomicLong(System.nanoTime() & 0x7FFFFFFFFFFFFFFFL);

    private SalesforceIdGenerator() {}

    /**
     * Generate a valid 18-character Salesforce ID with the given key prefix.
     */
    public static String generate(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.length() != 3) {
            throw new IllegalArgumentException("Key prefix must be exactly 3 characters");
        }
        String body = generateBody();
        String base15 = keyPrefix + body;
        String suffix = computeSuffix(base15);
        return base15 + suffix;
    }

    /**
     * Return the 15-char base from an 18-char ID (strip the suffix).
     * If the ID is already 15 chars, return as-is.
     */
    public static String to15(String id) {
        if (id == null) {
            return null;
        }
        if (id.length() == 18) {
            return id.substring(0, 15);
        }
        return id;
    }

    /**
     * Convert a 15-char ID to 18-char by computing and appending the suffix.
     * If the ID is already 18 chars, return as-is.
     */
    public static String to18(String id) {
        if (id == null) {
            return null;
        }
        if (id.length() == 18) {
            return id;
        }
        if (id.length() == 15) {
            return id + computeSuffix(id);
        }
        return id;
    }

    /**
     * Compare two Salesforce IDs for equality, accepting both 15 and 18-char forms.
     */
    public static boolean idsEqual(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return to15(a).equalsIgnoreCase(to15(b));
    }

    /**
     * Check whether a string looks like a valid Salesforce ID (15 or 18 chars,
     * alphanumeric only).
     */
    public static boolean isValidFormat(String id) {
        if (id == null) {
            return false;
        }
        if (id.length() != 15 && id.length() != 18) {
            return false;
        }
        for (char c : id.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compute the 3-character case-fold suffix for a 15-character base ID.
     */
    public static String computeSuffix(String base15) {
        if (base15.length() != 15) {
            throw new IllegalArgumentException("Base ID must be exactly 15 characters, got " + base15.length());
        }
        StringBuilder suffix = new StringBuilder(3);
        for (int chunk = 0; chunk < 3; chunk++) {
            int value = 0;
            for (int bit = 0; bit < 5; bit++) {
                char c = base15.charAt(chunk * 5 + bit);
                if (Character.isUpperCase(c)) {
                    value |= (1 << bit);
                }
            }
            suffix.append(SUFFIX_CHARS.charAt(value));
        }
        return suffix.toString();
    }

    private static String generateBody() {
        long value = COUNTER.getAndIncrement();
        StringBuilder body = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            body.append(BODY_CHARS.charAt((int) (Math.abs(value) % BODY_CHARS.length())));
            value = value / BODY_CHARS.length() + (value * 31 + 17);
        }
        return body.toString();
    }
}
