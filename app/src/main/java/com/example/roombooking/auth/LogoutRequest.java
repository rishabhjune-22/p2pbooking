package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class LogoutRequest {

    @SerializedName("refresh")
    private final String refresh;

    public LogoutRequest(String refresh) {
        this.refresh = refresh;
    }
}
