package com.example.roombooking.model.common;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ApiResponse<T> {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    @Nullable
    private T data;

    @SerializedName("errors")
    @Nullable
    private Map<String, List<String>> errors;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public String getFirstErrorMessage() {
        if (errors == null || errors.isEmpty()) {
            return message != null ? message : "Something went wrong";
        }

        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }

        return message != null ? message : "Something went wrong";
    }
}