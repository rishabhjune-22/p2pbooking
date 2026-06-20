package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenResponse {

    @SerializedName("access")
    private String access;

    public String getAccess() {
        return access != null ? access : "";
    }
}
