package com.example.roombooking.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.auth.LoginActivity;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.requester.RequesterLandingActivity;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.InternetErrorBanner;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;
    private static final long PRELOAD_TIMEOUT_MS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private RoomRepository roomRepository;
    private AuthSessionManager authSessionManager;

    private boolean minimumDelayCompleted = false;
    private boolean preloadCompleted = false;
    private boolean preloadTimeoutCompleted = false;
    private boolean navigationDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initDependencies();
        startSplashDelayTimer();
        if (authSessionManager.isLoggedIn()
                && authSessionManager.isApproved()
                && authSessionManager.isAdminLike()) {
            startPreloadTimeoutTimer();
            preloadRooms();
        } else {
            preloadCompleted = true;
        }
    }

    private void initDependencies() {
        roomRepository = new RoomRepository(getApplicationContext());
        authSessionManager = new AuthSessionManager(getApplicationContext());
    }

    private void startSplashDelayTimer() {
        handler.postDelayed(() -> {
            minimumDelayCompleted = true;
            tryNavigateNext();
        }, SPLASH_DELAY_MS);
    }

    private void startPreloadTimeoutTimer() {
        handler.postDelayed(() -> {
            preloadTimeoutCompleted = true;
            tryNavigateNext();
        }, PRELOAD_TIMEOUT_MS);
    }

    private void preloadRooms() {
        roomRepository.getRooms(result -> {
            if (isFinishing() || isDestroyed()) return;

            if (result.isSuccess()) {
                InternetErrorBanner.hide(this);
            } else if (InternetErrorBanner.isNetworkErrorMessage(result.getErrorMessage())) {
                InternetErrorBanner.show(this);
            }

            preloadCompleted = true;
            tryNavigateNext();
        });
    }

    private void tryNavigateNext() {
        if (isFinishing() || isDestroyed()) return;
        if (navigationDone) return;

        if (!minimumDelayCompleted) return;

        boolean canMoveAhead = preloadCompleted || preloadTimeoutCompleted;

        if (!canMoveAhead) return;

        if (!authSessionManager.isLoggedIn() || !authSessionManager.isApproved()) {
            navigateToLogin();
            return;
        }

        navigateToRoleLanding();
    }

    private void navigateToRoleLanding() {
        if (navigationDone) return;

        navigationDone = true;

        Class<?> destination = authSessionManager.isRequester()
                ? RequesterLandingActivity.class
                : LandingActivity.class;
        Intent intent = new Intent(SplashActivity.this, destination);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        if (navigationDone) return;

        navigationDone = true;

        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
