package com.example.roombooking.utils;

import android.content.Context;

import com.example.roombooking.auth.AuthSessionManager;
import com.example.roombooking.cache.LocalJsonCacheStore;

public final class ListScreenCache {

    private static final String ADMIN_BOOKING_REQUESTS_PREFIX = "admin:booking_requests:";
    private static final String ADMIN_REQUESTER_ACCOUNTS_PREFIX = "admin:requester_accounts:";
    private static final String SUPERADMIN_USER_PROFILES_PREFIX = "superadmin:user_profiles:";
    private static final String REQUESTER_MY_REQUESTS_PREFIX = "requester:my_requests:";

    private ListScreenCache() {
        // Utility class.
    }

    public static String adminBookingRequestsKey(Context context) {
        return ADMIN_BOOKING_REQUESTS_PREFIX + userId(context);
    }

    public static String adminRequesterAccountsKey(Context context) {
        return ADMIN_REQUESTER_ACCOUNTS_PREFIX + userId(context);
    }

    public static String superadminUserProfilesKey(Context context) {
        return SUPERADMIN_USER_PROFILES_PREFIX + userId(context);
    }

    public static String requesterMyRequestsKey(Context context) {
        return REQUESTER_MY_REQUESTS_PREFIX + userId(context);
    }

    public static void clearListScreenCaches(LocalJsonCacheStore cacheStore) {
        if (cacheStore == null) {
            return;
        }

        cacheStore.deleteByPrefix(ADMIN_BOOKING_REQUESTS_PREFIX);
        cacheStore.deleteByPrefix(ADMIN_REQUESTER_ACCOUNTS_PREFIX);
        cacheStore.deleteByPrefix(SUPERADMIN_USER_PROFILES_PREFIX);
        cacheStore.deleteByPrefix(REQUESTER_MY_REQUESTS_PREFIX);
    }

    public static boolean isStale(long updatedAtMillis, long freshnessWindowMs) {
        if (updatedAtMillis <= 0L || freshnessWindowMs <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - updatedAtMillis > freshnessWindowMs;
    }

    public static String lastUpdatedStatus(long updatedAtMillis) {
        return "Last updated " + relativeAge(updatedAtMillis);
    }

    public static String savedDataStatus(long updatedAtMillis) {
        return "Showing saved data. " + lastUpdatedStatus(updatedAtMillis) + ".";
    }

    public static String emptyStatus(String emptyMessage, long updatedAtMillis) {
        return emptyMessage + " " + lastUpdatedStatus(updatedAtMillis) + ".";
    }

    private static int userId(Context context) {
        return new AuthSessionManager(context.getApplicationContext()).getUserId();
    }

    private static String relativeAge(long updatedAtMillis) {
        if (updatedAtMillis <= 0L) {
            return "unknown";
        }

        long ageMillis = Math.max(0L, System.currentTimeMillis() - updatedAtMillis);
        long minutes = ageMillis / 60_000L;
        if (minutes <= 0L) {
            return "just now";
        }
        if (minutes == 1L) {
            return "1 min ago";
        }
        if (minutes < 60L) {
            return minutes + " mins ago";
        }

        long hours = minutes / 60L;
        if (hours == 1L) {
            return "1 hour ago";
        }
        if (hours < 24L) {
            return hours + " hours ago";
        }

        long days = hours / 24L;
        if (days == 1L) {
            return "1 day ago";
        }
        return days + " days ago";
    }

}
