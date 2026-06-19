package com.example.roombooking.cache;

public final class CachePolicy {

    public static final long ROOMS_TTL_MS = 24L * 60L * 60L * 1000L;
    public static final long BOOKING_PAGE_ONE_TTL_MS = 45L * 1000L;
    public static final long CALENDAR_AVAILABILITY_TTL_MS = 20L * 1000L;
    public static final long AVAILABLE_ROOMS_TTL_MS = 12L * 1000L;
    public static final long AVAILABLE_ROOMS_RANGE_TTL_MS = 12L * 1000L;

    private CachePolicy() {
        // Utility class. No object required.
    }

    public static boolean isFresh(
            long updatedAtMillis,
            long ttlMillis,
            long nowMillis
    ) {
        if (updatedAtMillis <= 0L || ttlMillis <= 0L) {
            return false;
        }

        long ageMillis = ageMillis(updatedAtMillis, nowMillis);
        return ageMillis >= 0L && ageMillis < ttlMillis;
    }

    public static long ageMillis(long updatedAtMillis, long nowMillis) {
        return nowMillis - updatedAtMillis;
    }
}
