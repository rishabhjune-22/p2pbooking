package com.example.roombooking.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.roombooking.auth.RefreshRequest;
import com.example.roombooking.auth.RefreshResponse;
import com.example.roombooking.auth.SessionManager;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class TokenAuthenticator implements Authenticator {

    private final SessionManager sessionManager;

    public TokenAuthenticator(Context context) {
        sessionManager = new SessionManager(context);
    }

    @Override
    public Request authenticate(Route route, @NonNull Response response) throws IOException {
        if (responseCount(response) >= 2) {
            return null;
        }

        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            sessionManager.logout();
            return null;
        }

        synchronized (this) {
            String currentAccessToken = sessionManager.getAccessToken();
            String requestAuthHeader = response.request().header("Authorization");

            if (requestAuthHeader != null && currentAccessToken != null) {
                String latestHeader = "Bearer " + currentAccessToken;
                if (!latestHeader.equals(requestAuthHeader)) {
                    return response.request().newBuilder()
                            .header("Authorization", latestHeader)
                            .build();
                }
            }

            Call<RefreshResponse> call = TokenRefreshClient.getApi()
                    .refreshToken(new RefreshRequest(refreshToken));

            retrofit2.Response<RefreshResponse> refreshResponse = call.execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                RefreshResponse body = refreshResponse.body();
                String newAccessToken = body.getAccess();
                String newRefreshToken = body.getRefresh();

                if (newAccessToken != null && !newAccessToken.trim().isEmpty()) {
                    sessionManager.updateAccessToken(newAccessToken);

                    if (newRefreshToken != null && !newRefreshToken.trim().isEmpty()) {
                        sessionManager.updateRefreshToken(newRefreshToken);
                    }

                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + newAccessToken)
                            .build();
                }
            }

            sessionManager.logout();
            return null;
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while (response.priorResponse() != null) {
            result++;
            response = response.priorResponse();
        }
        return result;
    }
}