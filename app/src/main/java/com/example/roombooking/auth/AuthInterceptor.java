package com.example.roombooking.auth;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {

    private final AuthSessionManager sessionManager;

    public AuthInterceptor(Context context) {
        sessionManager = new AuthSessionManager(context.getApplicationContext());
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
        return chain.proceed(authenticatedRequest);
    }
}
