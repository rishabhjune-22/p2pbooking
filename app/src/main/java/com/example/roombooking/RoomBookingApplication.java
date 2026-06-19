package com.example.roombooking;

import android.app.Application;

import com.example.roombooking.sync.LightBackgroundSyncScheduler;
import com.example.roombooking.utils.AppDiagnostics;

public class RoomBookingApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppDiagnostics.installCrashHandler();
        AppDiagnostics.logEvent("app_start");
        LightBackgroundSyncScheduler.schedule(this);
    }
}
