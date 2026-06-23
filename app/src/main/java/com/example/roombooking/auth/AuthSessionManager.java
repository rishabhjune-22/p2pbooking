package com.example.roombooking.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class AuthSessionManager {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_REQUESTER = "requester";
    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String APPROVAL_PENDING = "pending";
    public static final String APPROVAL_APPROVED = "approved";
    public static final String APPROVAL_REJECTED = "rejected";

    private static final String PREF_NAME = "auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_APPROVAL_STATUS = "approval_status";
    private static final String KEY_USER_DESIGNATION = "user_designation";
    private static final String KEY_USER_DEPARTMENT = "user_department";
    private static final String KEY_USER_MOBILE = "user_mobile";
    private static final String KEY_SESSION_EXPIRED = "session_expired";

    private final SharedPreferences prefs;

    public AuthSessionManager(Context context) {
        prefs = createPreferences(context.getApplicationContext());
    }

    private SharedPreferences createPreferences(Context appContext) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    appContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException | RuntimeException ignored) {
            return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(getAccessToken())
                && !TextUtils.isEmpty(getRefreshToken());
    }

    public String getAccessToken() {
        return clean(prefs.getString(KEY_ACCESS_TOKEN, ""));
    }

    public String getRefreshToken() {
        return clean(prefs.getString(KEY_REFRESH_TOKEN, ""));
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public String getUserName() {
        return clean(prefs.getString(KEY_USER_NAME, ""));
    }

    public String getUserEmail() {
        return clean(prefs.getString(KEY_USER_EMAIL, ""));
    }

    public String getUserRole() {
        String role = clean(prefs.getString(KEY_USER_ROLE, ROLE_ADMIN)).toLowerCase();
        return role.isEmpty() ? ROLE_ADMIN : role;
    }

    public String getApprovalStatus() {
        String status = clean(prefs.getString(KEY_APPROVAL_STATUS, APPROVAL_APPROVED))
                .toLowerCase();
        return status.isEmpty() ? APPROVAL_APPROVED : status;
    }

    public String getUserDesignation() {
        return clean(prefs.getString(KEY_USER_DESIGNATION, ""));
    }

    public String getUserDepartment() {
        return clean(prefs.getString(KEY_USER_DEPARTMENT, ""));
    }

    public String getUserMobile() {
        return clean(prefs.getString(KEY_USER_MOBILE, ""));
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(getUserRole());
    }

    public boolean isSuperadmin() {
        return ROLE_SUPERADMIN.equals(getUserRole());
    }

    public boolean isAdminLike() {
        return isAdmin() || isSuperadmin();
    }

    public boolean isRequester() {
        return ROLE_REQUESTER.equals(getUserRole());
    }

    public boolean isApproved() {
        return APPROVAL_APPROVED.equals(getApprovalStatus());
    }

    public boolean isSessionExpired() {
        return prefs.getBoolean(KEY_SESSION_EXPIRED, false);
    }

    public void saveSession(AuthResponse response) {
        if (response == null || response.getUser() == null) {
            return;
        }

        saveSession(
                response.getAccess(),
                response.getRefresh(),
                response.getUser()
        );
    }

    public void saveSession(String accessToken, String refreshToken, AuthUser user) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, clean(accessToken))
                .putString(KEY_REFRESH_TOKEN, clean(refreshToken))
                .putInt(KEY_USER_ID, user != null ? user.getId() : 0)
                .putString(KEY_USER_NAME, user != null ? user.getName() : "")
                .putString(KEY_USER_EMAIL, user != null ? user.getEmail() : "")
                .putString(KEY_USER_ROLE, normalizeRole(user != null ? user.getRole() : ""))
                .putString(KEY_APPROVAL_STATUS, normalizeApprovalStatus(
                        user != null ? user.getApprovalStatus() : ""
                ))
                .putString(KEY_USER_DESIGNATION, user != null ? user.getDesignation() : "")
                .putString(KEY_USER_DEPARTMENT, user != null ? user.getDepartment() : "")
                .putString(KEY_USER_MOBILE, user != null ? user.getMobile() : "")
                .putBoolean(KEY_SESSION_EXPIRED, false)
                .apply();
    }

    public void updateAccessToken(String accessToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, clean(accessToken))
                .putBoolean(KEY_SESSION_EXPIRED, false)
                .apply();
    }

    public void clearSession() {
        prefs.edit()
                .clear()
                .apply();
    }

    public void markSessionExpired() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .putBoolean(KEY_SESSION_EXPIRED, true)
                .apply();
    }

    public void clearSessionExpired() {
        prefs.edit()
                .putBoolean(KEY_SESSION_EXPIRED, false)
                .apply();
    }

    private static String clean(String value) {
        return value != null ? value.trim() : "";
    }

    private static String normalizeRole(String role) {
        String normalizedRole = clean(role).toLowerCase();
        if (ROLE_SUPERADMIN.equals(normalizedRole)) {
            return ROLE_SUPERADMIN;
        }
        if (ROLE_REQUESTER.equals(normalizedRole)) {
            return ROLE_REQUESTER;
        }
        return ROLE_ADMIN;
    }

    private static String normalizeApprovalStatus(String status) {
        String normalizedStatus = clean(status).toLowerCase();
        if (APPROVAL_PENDING.equals(normalizedStatus)
                || APPROVAL_REJECTED.equals(normalizedStatus)) {
            return normalizedStatus;
        }
        return APPROVAL_APPROVED;
    }
}
