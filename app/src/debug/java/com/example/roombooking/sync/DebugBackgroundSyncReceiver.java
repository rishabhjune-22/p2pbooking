package com.example.roombooking.sync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.roombooking.utils.AppDiagnostics;

public class DebugBackgroundSyncReceiver extends BroadcastReceiver {

    public static final String ACTION_RUN_BACKGROUND_SYNC =
            "com.example.roombooking.DEBUG_RUN_BACKGROUND_SYNC";
    public static final String UNIQUE_WORK_NAME =
            "room_booking_debug_one_time_background_sync";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        if (!ACTION_RUN_BACKGROUND_SYNC.equals(intent.getAction())) {
            return;
        }

        AppDiagnostics.logEvent("debug_background_sync_requested");

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                LightBackgroundSyncWorker.class
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        UNIQUE_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        request
                );

        AppDiagnostics.logEvent(
                "debug_background_sync_enqueued name=" + UNIQUE_WORK_NAME
        );
    }
}
