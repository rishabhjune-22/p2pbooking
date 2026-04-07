package com.example.roombooking;

public class LogoutRequest {
    private String refresh;

    public LogoutRequest(String refresh) {
        this.refresh = refresh;
    }

    public String getRefresh() {
        return refresh;
    }
}