package com.example.roombooking.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.auth.AuthActivity;
import com.example.roombooking.auth.SessionManager;
import com.example.roombooking.home.HomeActivity;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.security.KeystoreBackedCryptoSessionManager;
import com.example.roombooking.security.UnlockCryptoActivity;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 900L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private SessionManager sessionManager;
    private RoomRepository roomRepository;

    private boolean minimumDelayCompleted = false;
    private boolean preloadCompleted = false;
    private boolean navigationDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(getApplicationContext());
        roomRepository = new RoomRepository(getApplicationContext());

        handler.postDelayed(() -> {
            minimumDelayCompleted = true;
            tryNavigateNext();
        }, SPLASH_DELAY_MS);

        if (sessionManager.isLoggedIn()) {
            preloadRooms();
        } else {
            preloadCompleted = true;
        }
    }

    private void preloadRooms() {
        roomRepository.getRooms(result -> {
            preloadCompleted = true;
            tryNavigateNext();
        });
    }

    private void tryNavigateNext() {
        if (navigationDone) {
            return;
        }

        if (!minimumDelayCompleted || !preloadCompleted) {
            return;
        }

        navigationDone = true;

        Intent intent;

        if (!sessionManager.isLoggedIn()) {
            intent = new Intent(SplashActivity.this, AuthActivity.class);
        } else {
            boolean restored = KeystoreBackedCryptoSessionManager
                    .getInstance(getApplicationContext())
                    .restoreDekFromLocalStore();

            if (restored) {
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, UnlockCryptoActivity.class);
            }
        }

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