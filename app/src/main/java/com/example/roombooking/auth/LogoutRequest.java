package com.example.roombooking.auth;

public class LogoutRequest {
    private String refresh;

    public LogoutRequest(String refresh) {
        this.refresh = refresh;
    }

    public String getRefresh() {
        return refresh;
    }
}