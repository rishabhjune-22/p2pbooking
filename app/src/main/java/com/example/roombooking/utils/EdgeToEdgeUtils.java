package com.example.roombooking.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class EdgeToEdgeUtils {

    private EdgeToEdgeUtils() {
        // Utility class. No object required.
    }

    public static void applySystemBarInsets(Activity activity, View rootView) {
        if (activity == null || rootView == null) {
            return;
        }

        configureStatusBar(activity);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        int initialLeft = rootView.getPaddingLeft();
        int initialTop = rootView.getPaddingTop();
        int initialRight = rootView.getPaddingRight();
        int initialBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            view.setPadding(
                    initialLeft + systemBars.left,
                    initialTop + systemBars.top,
                    initialRight + systemBars.right,
                    initialBottom + systemBars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }

    public static void applyBottomInsetOnly(Activity activity, View targetView) {
        if (activity == null || targetView == null) {
            return;
        }

        configureStatusBar(activity);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        int initialLeft = targetView.getPaddingLeft();
        int initialTop = targetView.getPaddingTop();
        int initialRight = targetView.getPaddingRight();
        int initialBottom = targetView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(targetView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            view.setPadding(
                    initialLeft,
                    initialTop,
                    initialRight,
                    initialBottom + systemBars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(targetView);
    }

    private static void configureStatusBar(Activity activity) {
        Window window = activity.getWindow();
        window.setStatusBarColor(Color.WHITE);
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(true);
    }
}
