package com.example.roombooking.auth;

import com.google.gson.annotations.SerializedName;

public class AuthUser {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    public int getId() {
        return id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public String getEmail() {
        return email != null ? email : "";
    }
}
