package com.example.roombooking.model.auth;

import com.example.roombooking.model.auth.UserData;
import com.google.gson.annotations.SerializedName;

public class LoginData {

    @SerializedName("refresh")
    private String refreshToken;

    @SerializedName("access")
    private String accessToken;

    @SerializedName("user")
    private UserData user;

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UserData getUser() {
        return user;
    }
}