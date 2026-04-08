package com.example.roombooking.api;

import android.content.Context;

import com.example.roombooking.auth.SessionManager;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        sessionManager = new SessionManager(context);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        HttpUrl url = original.url();
        String path = url.encodedPath();

        boolean skipAuth =
                path.equals("/api/token/") ||
                        path.equals("/api/token/refresh/") ||
                        path.equals("/api/accounts/signup/");

        if (skipAuth) {
            return chain.proceed(original);
        }

        String token = sessionManager.getAccessToken();

        if (token == null || token.trim().isEmpty()) {
            return chain.proceed(original);
        }

        Request authenticatedRequest = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }
}