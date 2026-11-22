package econest.util;

import econest.model.User;

/** Remembers who is logged in while the application is running. */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /** Username of the logged-in user, used for audit columns like created_by. */
    public static String username() {
        return currentUser == null ? "system" : currentUser.getUsername();
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static void clear() {
        currentUser = null;
    }
}
