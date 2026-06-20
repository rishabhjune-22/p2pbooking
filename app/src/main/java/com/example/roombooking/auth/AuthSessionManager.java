package com.example.roombooking.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class AuthSessionManager {

    private static final String PREF_NAME = "auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
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
}
