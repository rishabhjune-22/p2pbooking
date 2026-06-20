package com.example.roombooking.auth;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

public final class AuthActivityTracker implements Application.ActivityLifecycleCallbacks {

    private static final Object LOCK = new Object();
    private static WeakReference<Activity> currentActivityRef = new WeakReference<>(null);
    private static int startedActivityCount = 0;

    private AuthActivityTracker() {
    }

    public static void register(Application application) {
        application.registerActivityLifecycleCallbacks(new AuthActivityTracker());
    }

    public static boolean openLoginIfForeground(String message) {
        Activity activity;
        synchronized (LOCK) {
            activity = currentActivityRef.get();
            if (startedActivityCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }
        }

        if (activity instanceof LoginActivity || activity instanceof SignupActivity) {
            return false;
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                AuthSessionGuard.openLogin(activity, message);
            }
        });
        return true;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        updateCurrentActivity(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        synchronized (LOCK) {
            startedActivityCount++;
            currentActivityRef = new WeakReference<>(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        updateCurrentActivity(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // No action required.
    }

    @Override
    public void onActivityStopped(Activity activity) {
        synchronized (LOCK) {
            if (startedActivityCount > 0) {
                startedActivityCount--;
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No action required.
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        synchronized (LOCK) {
            Activity currentActivity = currentActivityRef.get();
            if (currentActivity == activity) {
                currentActivityRef = new WeakReference<>(null);
            }
        }
    }

    private static void updateCurrentActivity(Activity activity) {
        synchronized (LOCK) {
            currentActivityRef = new WeakReference<>(activity);
        }
    }
}
