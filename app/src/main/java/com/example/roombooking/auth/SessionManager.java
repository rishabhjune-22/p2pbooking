package com.example.roombooking.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF = "room_booking_prefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // -----------------------------
    // TOKENS
    // -----------------------------
    public void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putBoolean(KEY_LOGGED_IN, hasText(accessToken));
        editor.apply();
    }

    public String getAccessToken() {
        return sp.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return sp.getString(KEY_REFRESH_TOKEN, null);
    }

    public void updateAccessToken(String newAccessToken) {
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, newAccessToken)
                .putBoolean(KEY_LOGGED_IN, hasText(newAccessToken))
                .apply();
    }

    public void updateRefreshToken(String newRefreshToken) {
        sp.edit().putString(KEY_REFRESH_TOKEN, newRefreshToken).apply();
    }

    public boolean hasAccessToken() {
        return hasText(getAccessToken());
    }

    public boolean hasRefreshToken() {
        return hasText(getRefreshToken());
    }

    // -----------------------------
    // USER INFO (optional cache)
    // -----------------------------
    public void saveUserInfo(String username, String email) {
        sp.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public String getUsername() {
        return sp.getString(KEY_USERNAME, null);
    }

    public String getEmail() {
        return sp.getString(KEY_EMAIL, null);
    }

    // -----------------------------
    // LOGIN STATE
    // -----------------------------
    public boolean isLoggedIn() {
        return hasText(getAccessToken());
    }

    public void logout() {
        sp.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .putBoolean(KEY_LOGGED_IN, false)
                .apply();
    }

    public void clearSession() {
        sp.edit().clear().apply();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}