package com.example.roombooking.common;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roombooking.R;
import com.example.roombooking.booking.LandingActivity;
import com.example.roombooking.room.RoomRepository;
import com.example.roombooking.utils.InternetErrorBanner;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500L;
    private static final long PRELOAD_TIMEOUT_MS = 2500L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private RoomRepository roomRepository;
    private LocalUserManager localUserManager;

    private boolean minimumDelayCompleted = false;
    private boolean preloadCompleted = false;
    private boolean preloadTimeoutCompleted = false;
    private boolean navigationDone = false;
    private boolean userDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initDependencies();
        startSplashDelayTimer();
        startPreloadTimeoutTimer();
        preloadRooms();
    }

    private void initDependencies() {
        roomRepository = new RoomRepository(getApplicationContext());
        localUserManager = new LocalUserManager(getApplicationContext());
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
        if (navigationDone || userDialogShowing) return;

        if (!minimumDelayCompleted) return;

        boolean canMoveAhead = preloadCompleted || preloadTimeoutCompleted;

        if (!canMoveAhead) return;

        if (!localUserManager.hasUserName()) {
            showUserNameDialog();
            return;
        }

        navigateToLanding();
    }

    private void showUserNameDialog() {
        if (navigationDone || userDialogShowing) return;

        userDialogShowing = true;

        EditText input = new EditText(this);
        input.setHint(getString(R.string.hint_enter_name));
        input.setSingleLine(true);

        int padding = getDialogInputPadding();
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_welcome)
                .setMessage(R.string.dialog_message_enter_name)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(R.string.action_continue, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = input.getText() != null
                        ? input.getText().toString().trim()
                        : "";

                if (TextUtils.isEmpty(name)) {
                    input.setError(getString(R.string.error_name_required));
                    return;
                }

                localUserManager.saveUserName(name);
                dialog.dismiss();
                navigateToLanding();
            });
        });

        dialog.show();
    }

    private int getDialogInputPadding() {
        return (int) (20 * getResources().getDisplayMetrics().density);
    }

    private void navigateToLanding() {
        if (navigationDone) return;

        navigationDone = true;

        Intent intent = new Intent(SplashActivity.this, LandingActivity.class);
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
