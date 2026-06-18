package com.example.roombooking.model.booking;

import java.util.Locale;

public final class BookingStatus {

    public static final String ACTIVE = "active";
    public static final String EXPIRED = "expired";

    private BookingStatus() {
        // Utility class. No object required.
    }

    public static boolean isActive(String status) {
        return ACTIVE.equalsIgnoreCase(status);
    }

    public static boolean isExpired(String status) {
        return EXPIRED.equalsIgnoreCase(status);
    }

    public static String normalizeForList(String status) {
        if (status == null) {
            return ACTIVE;
        }

        String value = status.trim().toLowerCase(Locale.ROOT);
        return EXPIRED.equals(value) ? EXPIRED : ACTIVE;
    }

    public static String displayName(String status) {
        return isExpired(status) ? "Expired" : "Active";
    }
}
