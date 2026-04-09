package com.example.roombooking.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.roombooking.auth.RefreshRequest;
import com.example.roombooking.model.auth.RefreshTokenData;
import com.example.roombooking.auth.SessionManager;
import com.example.roombooking.model.common.ApiResponse;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

public class TokenAuthenticator implements Authenticator {

    private static final int MAX_AUTH_RETRY_COUNT = 2;

    private final Context appContext;
    private final SessionManager sessionManager;

    public TokenAuthenticator(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    @Override
    public Request authenticate(Route route, @NonNull Response response) throws IOException {
        if (responseCount(response) >= MAX_AUTH_RETRY_COUNT) {
            return null;
        }

        String refreshToken = sessionManager.getRefreshToken();
        if (!hasText(refreshToken)) {
            sessionManager.logout();
            return null;
        }

        synchronized (this) {
            String latestAccessToken = sessionManager.getAccessToken();
            String requestAuthorizationHeader = response.request().header("Authorization");

            if (hasText(latestAccessToken) && hasText(requestAuthorizationHeader)) {
                String latestBearerToken = buildBearerToken(latestAccessToken);
                if (!latestBearerToken.equals(requestAuthorizationHeader)) {
                    return response.request()
                            .newBuilder()
                            .header("Authorization", latestBearerToken)
                            .build();
                }
            }

            Call<ApiResponse<RefreshTokenData>> refreshCall =
                    RetrofitClient.getApiService(appContext)
                            .refreshToken(new RefreshRequest(refreshToken));

            retrofit2.Response<ApiResponse<RefreshTokenData>> refreshResponse = refreshCall.execute();

            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                sessionManager.logout();
                return null;
            }

            ApiResponse<RefreshTokenData> apiResponse = refreshResponse.body();
            if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                sessionManager.logout();
                return null;
            }

            String newAccessToken = apiResponse.getData().getAccessToken();
            if (!hasText(newAccessToken)) {
                sessionManager.logout();
                return null;
            }

            sessionManager.updateAccessToken(newAccessToken);

            return response.request()
                    .newBuilder()
                    .header("Authorization", buildBearerToken(newAccessToken))
                    .build();
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while (response.priorResponse() != null) {
            count++;
            response = response.priorResponse();
        }
        return count;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}