package com.example.roombooking.auth;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.roombooking.api.RetrofitClient;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public final class TokenRefreshAuthenticator implements Authenticator {

    private final Context appContext;
    private final AuthSessionManager sessionManager;
    private final Object refreshLock = new Object();

    public TokenRefreshAuthenticator(Context context) {
        appContext = context.getApplicationContext();
        sessionManager = new AuthSessionManager(appContext);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response) throws IOException {
        if (responseCount(response) > 1) {
            expireSession();
            return null;
        }

        if (isAuthEndpoint(response.request())) {
            return null;
        }

        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken.isEmpty()) {
            expireSession();
            return null;
        }

        synchronized (refreshLock) {
            String requestToken = tokenFromRequest(response.request());
            String currentToken = sessionManager.getAccessToken();
            if (!currentToken.isEmpty()
                    && requestToken != null
                    && !requestToken.equals(currentToken)) {
                return retryWithToken(response.request(), currentToken);
            }

            Call<RefreshTokenResponse> call = RetrofitClient
                    .getRefreshAuthApiService(appContext)
                    .refreshToken(new RefreshTokenRequest(refreshToken));
            retrofit2.Response<RefreshTokenResponse> refreshResponse = call.execute();
            if (!refreshResponse.isSuccessful()
                    || refreshResponse.body() == null
                    || refreshResponse.body().getAccess().isEmpty()) {
                expireSession();
                return null;
            }

            String newAccessToken = refreshResponse.body().getAccess();
            sessionManager.updateAccessToken(newAccessToken);
            return retryWithToken(response.request(), newAccessToken);
        }
    }

    private void expireSession() {
        sessionManager.markSessionExpired();
        AuthActivityTracker.openLoginIfForeground(AuthSessionGuard.SESSION_EXPIRED_MESSAGE);
    }

    private boolean isAuthEndpoint(Request request) {
        String path = request.url().encodedPath();
        return path.contains("/api/auth/login/")
                || path.contains("/api/auth/signup/")
                || path.contains("/api/auth/token/refresh/");
    }

    private Request retryWithToken(Request request, String accessToken) {
        return request.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
    }

    private String tokenFromRequest(Request request) {
        String header = request.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
