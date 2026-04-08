package com.example.roombooking.common;

import java.util.Map;

public class ApiErrorResponse {
    private String detail;
    private Map<String, Object> errors;

    public String getDetail() {
        return detail;
    }

    public Map<String, Object> getErrors() {
        return errors;
    }
}