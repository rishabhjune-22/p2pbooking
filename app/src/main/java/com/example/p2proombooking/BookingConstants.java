package com.example.p2proombooking;

public final class BookingConstants {

    private BookingConstants() {
        // Prevent instantiation
    }

    // -----------------------------
    // STATUS
    // -----------------------------
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELED = "CANCELED";
    public static final String STATUS_CONFLICTED = "CONFLICTED";

    // -----------------------------
    // SYNC FLAGS
    // -----------------------------
    public static final String SYNC_PENDING = "PENDING_SYNC";
    public static final String SYNCED = "SYNCED";

    // Optional (future ready)
    public static final String SYNC_FAILED = "SYNC_FAILED";
    public static final String SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS";
}
