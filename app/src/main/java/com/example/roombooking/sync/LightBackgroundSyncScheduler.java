package com.example.roombooking.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.roombooking.utils.AppDiagnostics;

import java.util.concurrent.TimeUnit;

public final class LightBackgroundSyncScheduler {

    public static final String UNIQUE_WORK_NAME =
            "room_booking_light_background_sync";
    public static final long SYNC_INTERVAL_HOURS = 12L;
    public static final long BACKOFF_DELAY_MINUTES = 30L;

    private static final ExistingPeriodicWorkPolicy WORK_POLICY =
            ExistingPeriodicWorkPolicy.KEEP;

    private LightBackgroundSyncScheduler() {
        // Utility class. No object required.
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        UNIQUE_WORK_NAME,
                        WORK_POLICY,
                        buildWorkRequest()
                );

        AppDiagnostics.logEvent(
                "background_sync_scheduled"
                        + " name=" + UNIQUE_WORK_NAME
                        + " intervalHours=" + SYNC_INTERVAL_HOURS
                        + " policy=KEEP"
        );
    }

    public static PeriodicWorkRequest buildWorkRequest() {
        return new PeriodicWorkRequest.Builder(
                LightBackgroundSyncWorker.class,
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
        )
                .setConstraints(buildConstraints())
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        BACKOFF_DELAY_MINUTES,
                        TimeUnit.MINUTES
                )
                .build();
    }

    public static Constraints buildConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }

    public static ExistingPeriodicWorkPolicy existingWorkPolicy() {
        return WORK_POLICY;
    }
}
