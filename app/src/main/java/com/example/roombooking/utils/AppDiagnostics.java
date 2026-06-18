package com.example.roombooking.utils;

import android.util.Log;

import androidx.annotation.Nullable;

public final class AppDiagnostics {

    private static final String TAG = "RoomBookingDiagnostics";
    private static volatile boolean crashHandlerInstalled = false;

    private AppDiagnostics() {
        // Utility class. No object required.
    }

    public static void installCrashHandler() {
        if (crashHandlerInstalled) {
            return;
        }

        synchronized (AppDiagnostics.class) {
            if (crashHandlerInstalled) {
                return;
            }

            Thread.UncaughtExceptionHandler previousHandler =
                    Thread.getDefaultUncaughtExceptionHandler();

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                Log.e(TAG, "Unhandled crash on thread " + thread.getName(), throwable);

                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            });

            crashHandlerInstalled = true;
        }
    }

    public static void logEvent(String eventName) {
        if (isBlank(eventName)) {
            return;
        }

        Log.i(TAG, "event=" + eventName.trim());
    }

    public static void logBookingMutationFailure(
            String action,
            @Nullable Integer bookingId,
            String message
    ) {
        logBookingMutationFailure(action, bookingId, message, null);
    }

    public static void logBookingMutationFailure(
            String action,
            @Nullable Integer bookingId,
            String message,
            @Nullable Throwable throwable
    ) {
        String logMessage = "booking_mutation_failed"
                + " action=" + safe(action)
                + " bookingId=" + (bookingId != null ? bookingId : "new")
                + " message=" + safe(message);

        if (throwable == null) {
            Log.w(TAG, logMessage);
        } else {
            Log.e(TAG, logMessage, throwable);
        }
    }

    public static void logApiFailure(
            String operation,
            String message,
            @Nullable Throwable throwable
    ) {
        String logMessage = "api_failure"
                + " operation=" + safe(operation)
                + " message=" + safe(message);

        if (throwable == null) {
            Log.w(TAG, logMessage);
        } else {
            Log.e(TAG, logMessage, throwable);
        }
    }

    private static String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
