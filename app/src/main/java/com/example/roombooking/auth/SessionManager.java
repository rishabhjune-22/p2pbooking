package com.example.roombooking.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class SessionManager {

    private static final String PREF_NAME = "room_booking_prefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(@Nullable String accessToken, @Nullable String refreshToken) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, normalize(accessToken))
                .putString(KEY_REFRESH_TOKEN, normalize(refreshToken))
                .apply();
    }

    public void saveUserInfo(@Nullable String username, @Nullable String email) {
        preferences.edit()
                .putString(KEY_USERNAME, normalize(username))
                .putString(KEY_EMAIL, normalize(email))
                .apply();
    }

    public void saveSession(
            @Nullable String accessToken,
            @Nullable String refreshToken,
            @Nullable String username,
            @Nullable String email
    ) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, normalize(accessToken))
                .putString(KEY_REFRESH_TOKEN, normalize(refreshToken))
                .putString(KEY_USERNAME, normalize(username))
                .putString(KEY_EMAIL, normalize(email))
                .apply();
    }

    @Nullable
    public String getAccessToken() {
        return preferences.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        return preferences.getString(KEY_REFRESH_TOKEN, null);
    }

    @Nullable
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, null);
    }

    @Nullable
    public String getEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }

    public void updateAccessToken(@Nullable String newAccessToken) {
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, normalize(newAccessToken))
                .apply();
    }

    public void updateRefreshToken(@Nullable String newRefreshToken) {
        preferences.edit()
                .putString(KEY_REFRESH_TOKEN, normalize(newRefreshToken))
                .apply();
    }

    public boolean hasAccessToken() {
        return hasText(getAccessToken());
    }

    public boolean hasRefreshToken() {
        return hasText(getRefreshToken());
    }

    public boolean isLoggedIn() {
        return hasAccessToken();
    }

    public void clearUserInfo() {
        preferences.edit()
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .apply();
    }

    public void clearTokens() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }

    public void logout() {
        preferences.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USERNAME)
                .remove(KEY_EMAIL)
                .apply();
    }

    public void clearAll() {
        preferences.edit().clear().apply();
    }

    private boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Nullable
    private String normalize(@Nullable String value) {
        return hasText(value) ? value.trim() : null;
    }
}