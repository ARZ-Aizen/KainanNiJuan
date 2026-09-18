package com.kainanresto.model;

public class User {
    private int userId;
    private String username;
    private String password;
    private Role role;
    private String fullName;
    private boolean active;

    public User() {
    }

    public User(String username, String password, Role role, String fullName, boolean active) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
    }

    public User(int userId, String username, String password, Role role, String fullName, boolean active) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", role=" + role +
                ", fullName='" + fullName + '\'' +
                ", active=" + active +
                '}';
    }
}