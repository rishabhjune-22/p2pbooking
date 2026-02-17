package com.example.p2proombooking;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class SessionManager {
    private static final String PREF = "p2proombooking_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LOGGED_IN_USER_ID = "logged_in_user_id";

    private final SharedPreferences sp;

    public SessionManager(Context ctx) {
        sp = ctx.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public String getOrCreateDeviceId() {
        String id = sp.getString(KEY_DEVICE_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            sp.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    public void setLoggedInUserId(String userId) {
        sp.edit().putString(KEY_LOGGED_IN_USER_ID, userId).apply();
    }

    public String getLoggedInUserId() {
        return sp.getString(KEY_LOGGED_IN_USER_ID, null);
    }

    public void logout() {
        sp.edit().remove(KEY_LOGGED_IN_USER_ID).apply();
    }
}