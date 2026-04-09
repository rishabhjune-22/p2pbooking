package com.example.roombooking.api;

import android.content.Context;

import com.example.roombooking.auth.SessionManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
            "/api/token/",
            "/api/token/refresh/",
            "/api/accounts/signup/"
    ));

    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context.getApplicationContext());
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String requestPath = originalRequest.url().encodedPath();

        if (shouldSkipAuthorization(requestPath)) {
            return chain.proceed(originalRequest);
        }

        String accessToken = sessionManager.getAccessToken();
        if (!hasText(accessToken)) {
            return chain.proceed(originalRequest);
        }

        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", buildBearerToken(accessToken))
                .build();

        return chain.proceed(authenticatedRequest);
    }

    private boolean shouldSkipAuthorization(String path) {
        return EXCLUDED_PATHS.contains(path);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}