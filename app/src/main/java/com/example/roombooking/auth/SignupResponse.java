package com.example.roombooking.auth;

import com.example.roombooking.model.UserDto;

public class SignupResponse {
    private String message;
    private UserDto user;

    public String getMessage() {
        return message;
    }

    public UserDto getUser() {
        return user;
    }
}