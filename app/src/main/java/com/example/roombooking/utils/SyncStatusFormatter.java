package com.example.roombooking.utils;

public final class SyncStatusFormatter {

    public static final String REFRESHING = "Refreshing...";
    public static final String SHOWING_CACHED_REFRESHING =
            "Showing cached data. Refreshing...";
    public static final String OFFLINE_SAVED_DATA =
            "Offline. Showing saved data.";
    public static final String FINAL_BOOKING_VERIFIED =
            "Final booking will be verified by server.";

    private static final long MINUTE_MS = 60L * 1000L;
    private static final long HOUR_MS = 60L * MINUTE_MS;

    private SyncStatusFormatter() {
        // Utility class. No object required.
    }

    public static String lastUpdated(long updatedAtMillis, long nowMillis) {
        if (updatedAtMillis <= 0L || nowMillis < updatedAtMillis) {
            return "";
        }

        long ageMillis = nowMillis - updatedAtMillis;
        if (ageMillis < MINUTE_MS) {
            return "Last updated just now";
        }

        if (ageMillis >= HOUR_MS) {
            long hours = ageMillis / HOUR_MS;
            if (hours == 1L) {
                return "Last updated 1 hr ago";
            }

            return "Last updated " + hours + " hrs ago";
        }

        long minutes = ageMillis / MINUTE_MS;
        if (minutes == 1L) {
            return "Last updated 1 min ago";
        }

        return "Last updated " + minutes + " mins ago";
    }

    public static String lastUpdated(long updatedAtMillis) {
        return lastUpdated(updatedAtMillis, System.currentTimeMillis());
    }

    public static String availabilityDecisionText(long updatedAtMillis) {
        String lastUpdated = lastUpdated(updatedAtMillis);
        if (lastUpdated.trim().isEmpty()) {
            return FINAL_BOOKING_VERIFIED;
        }

        return lastUpdated + "\n" + FINAL_BOOKING_VERIFIED;
    }

    public static String cachedAvailabilityRefreshing() {
        return "Showing cached availability. Refreshing...\n" + FINAL_BOOKING_VERIFIED;
    }

    public static String offlineAvailabilitySaved() {
        return OFFLINE_SAVED_DATA + "\n" + FINAL_BOOKING_VERIFIED;
    }
}
