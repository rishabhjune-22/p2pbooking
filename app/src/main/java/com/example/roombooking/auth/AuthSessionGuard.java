package com.example.roombooking.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.roombooking.booking.LandingActivity;

public final class AuthSessionGuard {

    public static final String EXTRA_SESSION_MESSAGE = "session_message";
    public static final String SESSION_EXPIRED_MESSAGE =
            "Session expired. Please login again.";
    public static final String REQUESTER_WEB_ONLY_MESSAGE =
            "Requester access is available from the web portal only.";

    private AuthSessionGuard() {
    }

    public static boolean ensureAuthenticated(Activity activity) {
        AuthSessionManager sessionManager = new AuthSessionManager(activity);
        if (sessionManager.isAdminLike() && sessionManager.isApproved()) {
            return true;
        }

        boolean requesterSession = sessionManager.isRequester();
        String message = requesterSession
                ? REQUESTER_WEB_ONLY_MESSAGE
                : sessionManager.isSessionExpired() ? SESSION_EXPIRED_MESSAGE : "";
        if (requesterSession) {
            sessionManager.clearSession();
        }
        openLogin(activity, message);
        return false;
    }

    public static boolean ensureAdmin(Activity activity) {
        AuthSessionManager sessionManager = new AuthSessionManager(activity);
        if (!sessionManager.isLoggedIn() || !sessionManager.isApproved()) {
            String message = sessionManager.isSessionExpired() ? SESSION_EXPIRED_MESSAGE : "";
            openLogin(activity, message);
            return false;
        }

        if (sessionManager.isAdminLike()) {
            return true;
        }

        sessionManager.clearSession();
        openLogin(activity, REQUESTER_WEB_ONLY_MESSAGE);
        return false;
    }

    public static boolean ensureRequester(Activity activity) {
        AuthSessionManager sessionManager = new AuthSessionManager(activity);
        sessionManager.clearSession();
        openLogin(activity, REQUESTER_WEB_ONLY_MESSAGE);
        return false;
    }

    public static void openLandingForSession(Activity activity) {
        AuthSessionManager sessionManager = new AuthSessionManager(activity);
        if (sessionManager.isAdminLike() && sessionManager.isApproved()) {
            openAdminLanding(activity);
            return;
        }
        sessionManager.clearSession();
        openLogin(activity, REQUESTER_WEB_ONLY_MESSAGE);
    }

    public static void openLogin(Activity activity, String message) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (message != null && !message.trim().isEmpty()) {
            intent.putExtra(EXTRA_SESSION_MESSAGE, message.trim());
        }
        activity.startActivity(intent);
        activity.finish();
    }

    private static void openAdminLanding(Activity activity) {
        Intent intent = new Intent(activity, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

}
