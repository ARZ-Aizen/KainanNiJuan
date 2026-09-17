package com.kainanresto.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    // RAW PASSWORD - > HASH (STORING IN DB) THEN HASH - > RAW PASSWORD (FINDING ON DB)

    // BEST CHOICE SA SECURE NA BALANCE LATENCY (~200ms) AND RESISTANCE SA BRUTE FORCE
    private static final int BCRYPT_WORK_FACTOR = 12;

    private PasswordHasher() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    // CHECKS RAW PASSWORD MATCH EXISTING BCRYPT HASH
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            //CATCH HASH FORMAT ERRORS WITHOUT CRASHING
            return false;
        }
    }
}