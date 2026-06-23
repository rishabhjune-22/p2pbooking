package com.example.roombooking.auth;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("user")
    private AuthUser user;

    @SerializedName("access")
    private String access;

    @SerializedName("refresh")
    private String refresh;

    public AuthUser getUser() {
        return user;
    }

    public String getAccess() {
        return access != null ? access : "";
    }

    public String getRefresh() {
        return refresh != null ? refresh : "";
    }

    public boolean hasTokens() {
        return !TextUtils.isEmpty(getAccess()) && !TextUtils.isEmpty(getRefresh());
    }
}
