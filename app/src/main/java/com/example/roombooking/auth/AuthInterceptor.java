package com.example.roombooking.auth;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    private static final String ACCESS_CHANGED_MESSAGE =
            "Your account access changed. Please login again.";

    private final Context appContext;
    private final AuthSessionManager sessionManager;

    public AuthInterceptor(Context context) {
        appContext = context.getApplicationContext();
        sessionManager = new AuthSessionManager(appContext);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String accessToken = sessionManager.getAccessToken();

        if (accessToken.isEmpty() || request.header("Authorization") != null) {
            return chain.proceed(request);
        }

        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
        Response response = chain.proceed(authenticatedRequest);
        if (shouldForceLogout(authenticatedRequest, response)) {
            AuthLogoutManager.clearLocalSessionAndSensitiveCache(appContext);
            AuthActivityTracker.openLoginIfForeground(ACCESS_CHANGED_MESSAGE);
        }
        return response;
    }

    private boolean shouldForceLogout(Request request, Response response) {
        if (response.code() != 401 && response.code() != 403) {
            return false;
        }
        return !isAuthEndpoint(request);
    }

    private boolean isAuthEndpoint(Request request) {
        String path = request.url().encodedPath();
        return path.startsWith("/api/auth/admin/login/")
                || path.startsWith("/api/auth/requester/login/")
                || path.startsWith("/api/auth/login/")
                || path.startsWith("/api/auth/admin/signup/")
                || path.startsWith("/api/auth/requester/signup/")
                || path.startsWith("/api/auth/signup/")
                || path.startsWith("/api/auth/token/refresh/")
                || path.startsWith("/api/auth/logout/");
    }
}
