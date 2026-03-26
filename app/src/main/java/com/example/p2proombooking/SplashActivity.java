package com.example.p2proombooking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager session = new SessionManager(this);
        session.getOrCreateDeviceId(); // ensures device identity exists

        String userId = session.getActiveUserId();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (userId != null) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, AuthActivity.class));
            }
            finish();
        }, 900); // keep short
    }
}
