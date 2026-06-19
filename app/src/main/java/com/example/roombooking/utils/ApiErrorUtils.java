package com.example.roombooking.utils;

import com.example.roombooking.model.common.ApiResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import retrofit2.Response;

public final class ApiErrorUtils {

    public static final String NETWORK_ERROR_MESSAGE =
            "Please check your internet connection.";
    public static final String NO_INTERNET_ERROR_MESSAGE =
            "No internet connection. Please check your connection.";
    public static final String SERVER_UNAVAILABLE_ERROR_MESSAGE =
            "Server unavailable. Please try again.";
    public static final String TIMEOUT_ERROR_MESSAGE =
            "Request timed out. Please try again.";
    public static final String RATE_LIMIT_ERROR_MESSAGE =
            "Too many requests. Please wait a moment and try again.";
    public static final String DEFAULT_ERROR_MESSAGE =
            "Something went wrong. Please try again.";

    private static final Gson GSON = new Gson();

    private ApiErrorUtils() {
        // Utility class. No object required.
    }

    public static <T> String messageFromResponse(
            Response<ApiResponse<T>> response,
            String fallbackMessage
    ) {
        if (response == null) {
            return fallback(fallbackMessage);
        }

        String effectiveFallback = response.code() == 429
                ? RATE_LIMIT_ERROR_MESSAGE
                : fallbackMessage;

        ApiResponse<T> body = response.body();
        if (body != null) {
            return firstAvailableMessage(body, effectiveFallback);
        }

        try {
            if (response.errorBody() == null) {
                return fallbackWithCode(effectiveFallback, response.code());
            }

            String errorJson = response.errorBody().string();
            ApiResponse<?> errorResponse = GSON.fromJson(errorJson, ApiResponse.class);

            if (errorResponse != null) {
                return firstAvailableMessage(errorResponse, effectiveFallback);
            }
        } catch (Exception ignored) {
            // Return fallback below.
        }

        return fallbackWithCode(effectiveFallback, response.code());
    }

    public static String networkMessage() {
        return NETWORK_ERROR_MESSAGE;
    }

    public static String messageFromThrowable(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return TIMEOUT_ERROR_MESSAGE;
        }

        if (throwable instanceof UnknownHostException) {
            return NO_INTERNET_ERROR_MESSAGE;
        }

        if (throwable instanceof ConnectException) {
            return SERVER_UNAVAILABLE_ERROR_MESSAGE;
        }

        if (throwable instanceof IOException) {
            return NETWORK_ERROR_MESSAGE;
        }

        return DEFAULT_ERROR_MESSAGE;
    }

    public static String messageForHttpCode(int httpCode) {
        if (httpCode == 429) {
            return RATE_LIMIT_ERROR_MESSAGE;
        }

        if (httpCode == 408) {
            return TIMEOUT_ERROR_MESSAGE;
        }

        if (httpCode >= 500 && httpCode <= 599) {
            return SERVER_UNAVAILABLE_ERROR_MESSAGE;
        }

        return DEFAULT_ERROR_MESSAGE;
    }

    public static String cachedDataMessageForHttpCode(int httpCode) {
        if (httpCode == 429) {
            return "Too many requests. Showing saved data.";
        }

        if (httpCode == 408) {
            return "Request timed out. Showing saved data.";
        }

        if (httpCode >= 500 && httpCode <= 599) {
            return "Server unavailable. Showing saved data.";
        }

        return SyncStatusFormatter.OFFLINE_SAVED_DATA;
    }

    public static String cachedDataMessageForThrowable(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException) {
            return "Request timed out. Showing saved data.";
        }

        if (throwable instanceof ConnectException) {
            return "Server unavailable. Showing saved data.";
        }

        return SyncStatusFormatter.OFFLINE_SAVED_DATA;
    }

    public static String rateLimitMessage() {
        return RATE_LIMIT_ERROR_MESSAGE;
    }

    public static String messageFromApiResponse(
            ApiResponse<?> response,
            String fallbackMessage
    ) {
        if (response == null) {
            return fallback(fallbackMessage);
        }

        return firstAvailableMessage(response, fallbackMessage);
    }

    private static String firstAvailableMessage(
            ApiResponse<?> response,
            String fallbackMessage
    ) {
        if (response.hasErrors()) {
            String firstError = response.getFirstErrorMessage();
            if (!isBlank(firstError)) {
                return firstError;
            }
        }

        String message = response.getMessage();
        if (!isBlank(message)) {
            return message;
        }

        return fallback(fallbackMessage);
    }

    private static String fallbackWithCode(String fallbackMessage, int code) {
        String fallback = fallback(fallbackMessage);
        return code > 0 ? fallback + " Code: " + code : fallback;
    }

    private static String fallback(String fallbackMessage) {
        return isBlank(fallbackMessage) ? DEFAULT_ERROR_MESSAGE : fallbackMessage;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
