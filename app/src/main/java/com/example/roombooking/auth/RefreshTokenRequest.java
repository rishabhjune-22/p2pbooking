package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenRequest {

    @SerializedName("refresh")
    private final String refresh;

    public RefreshTokenRequest(String refresh) {
        this.refresh = refresh;
    }
}
