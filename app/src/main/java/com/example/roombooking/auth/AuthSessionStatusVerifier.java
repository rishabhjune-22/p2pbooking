package com.example.roombooking.auth;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.model.common.ApiResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AuthSessionStatusVerifier {

    private static final long CHECK_THROTTLE_MS = 10_000L;
    private static final String REJECTED_MESSAGE = "Your account is rejected.";
    private static final Object LOCK = new Object();

    private static long lastCheckAt = 0L;
    private static boolean inFlight = false;

    private AuthSessionStatusVerifier() {
    }

    public static void verifyOnResume(Activity activity) {
        if (activity == null
                || activity.isFinishing()
                || activity.isDestroyed()
                || activity instanceof LoginActivity
                || activity instanceof SignupActivity) {
            return;
        }

        Context appContext = activity.getApplicationContext();
        AuthSessionManager sessionManager = new AuthSessionManager(appContext);
        if (!sessionManager.isLoggedIn() || !sessionManager.isApproved()) {
            return;
        }

        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (inFlight || now - lastCheckAt < CHECK_THROTTLE_MS) {
                return;
            }
            inFlight = true;
            lastCheckAt = now;
        }

        RetrofitClient.getAuthApiService(appContext).me().enqueue(new Callback<ApiResponse<AuthUser>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ApiResponse<AuthUser>> call,
                    @NonNull Response<ApiResponse<AuthUser>> response
            ) {
                markDone();
                if (response.code() == 401 || response.code() == 403) {
                    forceLogout(appContext, REJECTED_MESSAGE);
                    return;
                }

                ApiResponse<AuthUser> body = response.body();
                AuthUser user = body != null ? body.getData() : null;
                if (!response.isSuccessful() || body == null || !body.isSuccess() || user == null) {
                    return;
                }

                if (!AuthSessionManager.APPROVAL_APPROVED.equalsIgnoreCase(user.getApprovalStatus())) {
                    forceLogout(appContext, messageForStatus(user.getApprovalStatus()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthUser>> call, @NonNull Throwable t) {
                markDone();
            }
        });
    }

    private static void markDone() {
        synchronized (LOCK) {
            inFlight = false;
        }
    }

    private static String messageForStatus(String status) {
        if (AuthSessionManager.APPROVAL_REJECTED.equalsIgnoreCase(status)) {
            return REJECTED_MESSAGE;
        }
        if (AuthSessionManager.APPROVAL_PENDING.equalsIgnoreCase(status)) {
            return "Your account is pending approval.";
        }
        return AuthSessionGuard.SESSION_EXPIRED_MESSAGE;
    }

    private static void forceLogout(Context context, String message) {
        AuthLogoutManager.clearLocalSessionAndSensitiveCache(context.getApplicationContext());
        AuthActivityTracker.openLoginIfForeground(message);
    }
}
