package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class SignupRequest {

    @SerializedName("name")
    private final String name;

    @SerializedName("email")
    private final String email;

    @SerializedName("password")
    private final String password;

    @SerializedName("confirm_password")
    private final String confirmPassword;

    public SignupRequest(
            String name,
            String email,
            String password,
            String confirmPassword
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }
}
