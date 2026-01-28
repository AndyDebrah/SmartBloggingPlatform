
package com.smartblog.application.security;

import com.smartblog.core.model.User;

public final class SecurityContext {
    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static User getUser() { return currentUser; }
    public static void logout() { currentUser = null; }

    public static boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }

    public static boolean isAuthor() {
        return currentUser != null && "AUTHOR".equals(currentUser.getRole());
    }
}
