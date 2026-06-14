package com.example.roombooking.model.common;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ApiResponse<T> {

    private static final String DEFAULT_ERROR_MESSAGE = "Something went wrong";

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    @Nullable
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

    @Nullable
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

    public boolean hasData() {
        return data != null;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getFirstErrorMessage() {
        if (!hasErrors()) {
            return getSafeMessage();
        }

        for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
            List<String> values = entry.getValue();

            if (values != null && !values.isEmpty()) {
                String firstError = values.get(0);

                if (firstError != null && !firstError.trim().isEmpty()) {
                    return firstError;
                }
            }
        }

        return getSafeMessage();
    }

    public String getSafeMessage() {
        return message != null && !message.trim().isEmpty()
                ? message
                : DEFAULT_ERROR_MESSAGE;
    }
}