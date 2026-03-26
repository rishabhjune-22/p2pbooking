package com.example.p2proombooking;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySystemBarStyle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applySystemBarStyle();
    }

    protected void applySystemBarStyle() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        @ColorInt int systemBarColor = isDarkMode ? Color.BLACK : Color.WHITE;
        Window window = getWindow();
        window.setStatusBarColor(systemBarColor);
        window.setNavigationBarColor(systemBarColor);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDarkMode);
            controller.setAppearanceLightNavigationBars(!isDarkMode);
        }
    }
}
