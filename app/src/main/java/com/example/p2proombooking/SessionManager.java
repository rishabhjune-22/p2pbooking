package com.example.p2proombooking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class SessionManager {

    private static final String PREF = "p2proombooking_prefs";

    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_ACTIVE_USER_ID = "active_user_id";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // -----------------------------
    // DEVICE ID (immutable per install)
    // -----------------------------
    public String getOrCreateDeviceId() {
        String id = sp.getString(KEY_DEVICE_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    // -----------------------------
    // DISPLAY NAME (user chosen, cached)
    // -----------------------------
    public void setDisplayName(String name) {
        sp.edit().putString(KEY_DISPLAY_NAME, name).apply();
    }

    public String getDisplayName() {
        return sp.getString(KEY_DISPLAY_NAME, "Unknown");
    }

    // -----------------------------
    // ACTIVE USER (DB userId UUID)
    // -----------------------------
    public void setActiveUserId(String userId) {
        sp.edit().putString(KEY_ACTIVE_USER_ID, userId).apply();
    }

    public String getActiveUserId() {
        return sp.getString(KEY_ACTIVE_USER_ID, null);
    }

    // -----------------------------
    // LOGIN STATE
    // -----------------------------
    public void setLoggedIn(boolean loggedIn) {
        sp.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return sp.getBoolean(KEY_LOGGED_IN, false)
                && getActiveUserId() != null;
    }

    public void logout() {
        sp.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_ACTIVE_USER_ID)
                .apply();
    }
}
