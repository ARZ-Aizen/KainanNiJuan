package com.kainanresto.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {

    private static final int WORK_FACTOR = 12;

    private PasswordHasher() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    //PANG HASH NG NEW USER
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    //PANG UNHASH SA EXISTING USER
    public static boolean verify(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}