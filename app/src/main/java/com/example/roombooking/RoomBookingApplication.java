package com.example.roombooking;

import android.app.Application;

import com.example.roombooking.auth.AuthActivityTracker;
import com.example.roombooking.sync.LightBackgroundSyncScheduler;
import com.example.roombooking.utils.AppDiagnostics;

public class RoomBookingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AuthActivityTracker.register(this);
        AppDiagnostics.installCrashHandler();
        AppDiagnostics.logEvent("app_start");
        LightBackgroundSyncScheduler.schedule(this);
    }
}
