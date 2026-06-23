package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("expected_role")
    private final String expectedRole;

    public LoginRequest(String email, String password) {
        this(email, password, null);
    }

    public LoginRequest(String email, String password, String expectedRole) {
        this.email = email;
        this.password = password;
        this.expectedRole = expectedRole;
    }
}
