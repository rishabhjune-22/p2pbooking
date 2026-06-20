package com.example.roombooking.auth;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.work.WorkManager;

import com.example.roombooking.R;
import com.example.roombooking.api.RetrofitClient;
import com.example.roombooking.booking.AvailabilityRepository;
import com.example.roombooking.booking.BookingRepository;
import com.example.roombooking.booking.LandingViewModel;
import com.example.roombooking.cache.LocalJsonCacheStore;
import com.example.roombooking.home.HomeViewModel;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.sync.LightBackgroundSyncScheduler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AuthLogoutManager {

    private AuthLogoutManager() {
    }

    public static void confirmAndLogout(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout(activity))
                .show();
    }

    private static void logout(Activity activity) {
        AuthSessionManager sessionManager =
                new AuthSessionManager(activity.getApplicationContext());
        String refreshToken = sessionManager.getRefreshToken();

        if (!refreshToken.isEmpty()) {
            RetrofitClient.getAuthApiService(activity.getApplicationContext())
                    .logout(new LogoutRequest(refreshToken))
                    .enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(
                                @NonNull Call<ApiResponse<Void>> call,
                                @NonNull Response<ApiResponse<Void>> response
                        ) {
                            finishLogout(activity);
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<ApiResponse<Void>> call,
                                @NonNull Throwable t
                        ) {
                            finishLogout(activity);
                        }
                    });
            return;
        }

        finishLogout(activity);
    }

    private static void finishLogout(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            clearLocalSessionAndSensitiveCache(activity.getApplicationContext());
            return;
        }

        clearLocalSessionAndSensitiveCache(activity.getApplicationContext());
        Toast.makeText(activity, R.string.message_logged_out, Toast.LENGTH_SHORT).show();
        AuthSessionGuard.openLogin(activity, "");
    }

    public static void clearLocalSessionAndSensitiveCache(Context context) {
        Context appContext = context.getApplicationContext();
        new AuthSessionManager(appContext).clearSession();

        LocalJsonCacheStore cacheStore = new LocalJsonCacheStore(appContext);
        BookingRepository.clearFirstPageCaches(cacheStore);
        AvailabilityRepository.clearAvailabilityCaches(cacheStore);
        HomeViewModel.clearInMemoryFirstPageCacheForLogout();
        LandingViewModel.clearInMemoryAvailabilityCachesForLogout();

        WorkManager.getInstance(appContext)
                .cancelUniqueWork(LightBackgroundSyncScheduler.UNIQUE_WORK_NAME);
    }
}
