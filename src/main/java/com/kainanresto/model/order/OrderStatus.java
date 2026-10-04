package com.kainanresto.model.order;

import java.util.Locale;

public enum OrderStatus {
    PREPARING("Preparing"), COMPLETED("Completed"), CANCELLED("Cancelled");

    private final String displayName;
    OrderStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public static OrderStatus fromString(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OrderStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}