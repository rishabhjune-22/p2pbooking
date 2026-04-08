package com.example.roombooking.auth;

public class RefreshRequest {
    private String refresh;

    public RefreshRequest(String refresh) {
        this.refresh = refresh;
    }

    public String getRefresh() {
        return refresh;
    }
}