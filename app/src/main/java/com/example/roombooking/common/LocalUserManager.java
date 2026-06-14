package com.example.roombooking.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class LocalUserManager {

    private static final String PREF_NAME = "local_user_prefs";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public LocalUserManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserName(String name) {
        prefs.edit()
                .putString(KEY_USER_NAME, cleanText(name))
                .apply();
    }

    public String getUserName() {
        return cleanText(prefs.getString(KEY_USER_NAME, ""));
    }

    public boolean hasUserName() {
        return !TextUtils.isEmpty(getUserName());
    }

    public void clearUserName() {
        prefs.edit()
                .remove(KEY_USER_NAME)
                .apply();
    }

    private String cleanText(String value) {
        return value != null ? value.trim() : "";
    }
}