package com.kainanresto.model;

public enum Role {
    ADMIN,
    CASHIER,
    MANAGER,
    SUPERVISOR;

    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return CASHIER;
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CASHIER;
        }
    }
}