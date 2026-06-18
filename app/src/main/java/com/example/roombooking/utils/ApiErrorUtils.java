package com.example.roombooking.utils;

import com.example.roombooking.model.common.ApiResponse;
import com.google.gson.Gson;

import retrofit2.Response;

public final class ApiErrorUtils {

    public static final String NETWORK_ERROR_MESSAGE =
            "Please check your internet connection.";
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
