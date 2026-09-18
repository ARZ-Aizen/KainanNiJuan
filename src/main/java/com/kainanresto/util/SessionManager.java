package com.kainanresto.util;

import com.kainanresto.model.Role;
import com.kainanresto.model.User;

public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    //PAG STORE NG CURRENT USER SA MEM
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

   //RETRIEVE NG CURRENT USER
    public static User getCurrentUser() {
        return currentUser;
    }

    //PANGCHECK IF NAKA LOG IN
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    //VERIFY NG ROLE
    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    //CLEAR SESSION
    public static void clearSession() {
        currentUser = null;
    }
}