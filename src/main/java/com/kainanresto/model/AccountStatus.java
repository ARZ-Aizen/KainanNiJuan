package com.kainanresto.model;

public enum AccountStatus {
    ACTIVE("Active"), INACTIVE("Inactive"), SUSPENDED("Suspended");
    private final String displayName;
    AccountStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
