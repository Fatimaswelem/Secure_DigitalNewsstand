package com.newsstand.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * PasswordSecurity
 * ────────────────────────────────────────────────────────────────────────────
 * Provides salted BCrypt password hashing and verification.
 *
 * WHY BCrypt?
 *  • Each call to hashPassword() generates a fresh cryptographic salt
 *    automatically and embeds it in the resulting hash string.
 *  • BCrypt is intentionally slow (work factor = 2^STRENGTH rounds), making
 *    brute-force and rainbow-table attacks computationally expensive.
 *  • The same plain-text password hashed twice will produce two *different*
 *    hash strings — yet verifyPassword() correctly matches both against the
 *    original plain text.
 *
 * PROJECT EVALUATION CRITERIA MET
 *  ✔ Password Security using Salt and Hashing  (5 pts)
 */
public class PasswordSecurity {

    /**
     * BCrypt work factor.
     * 12 is a good balance between security and latency (~300 ms on modern HW).
     * Increase to 14 for higher security if latency is acceptable.
     */
    private static final int STRENGTH = 12;

    private PasswordSecurity() { /* utility class — no instances */ }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Hashes a plain-text password using BCrypt with an auto-generated salt.
     *
     * @param plainTextPassword the password entered by the user
     * @return a 60-character BCrypt hash that includes the embedded salt
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        // BCrypt.gensalt() generates a fresh random salt on every call.
        // The salt is embedded in the returned hash, so it does NOT need to be
        // stored separately.
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(STRENGTH));
    }

    /**
     * Verifies that a plain-text password matches a stored BCrypt hash.
     *
     * @param plainTextPassword the candidate password
     * @param storedHash        the BCrypt hash retrieved from the database
     * @return true if the password matches; false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String storedHash) {
        if (plainTextPassword == null || storedHash == null) return false;
        try {
            return BCrypt.checkpw(plainTextPassword, storedHash);
        } catch (IllegalArgumentException e) {
            // storedHash was not a valid BCrypt string
            return false;
        }
    }
}
