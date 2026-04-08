package com.example.roombooking.auth;

import java.util.List;
import java.util.Map;

public class SignupErrorResponse {
    private String message;
    private Map<String, List<String>> errors;

    public String getMessage() {
        return message;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}