package com.example.roombooking.model.auth;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenData {

    @SerializedName("access")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }
}