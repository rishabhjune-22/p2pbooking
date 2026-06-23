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

    @SerializedName("admin_code")
    private final String adminCode;

    @SerializedName("designation")
    private final String designation;

    @SerializedName("department")
    private final String department;

    @SerializedName("mobile")
    private final String mobile;

    public SignupRequest(
            String name,
            String email,
            String password,
            String confirmPassword
    ) {
        this(name, email, password, confirmPassword, null, null, null, null);
    }

    public SignupRequest(
            String name,
            String email,
            String password,
            String confirmPassword,
            String adminCode,
            String designation,
            String department,
            String mobile
    ) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.adminCode = adminCode;
        this.designation = designation;
        this.department = department;
        this.mobile = mobile;
    }
}
