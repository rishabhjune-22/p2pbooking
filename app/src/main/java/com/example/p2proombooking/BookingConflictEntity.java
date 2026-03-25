package com.example.p2proombooking;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "booking_conflicts")
public class BookingConflictEntity {

    @PrimaryKey
    @NonNull
    public String bookingId = "";

    @NonNull
    public String roomId = "";

    public long localStartUtc;
    public long localEndUtc;

    public long remoteStartUtc;
    public long remoteEndUtc;

    @NonNull
    public String remoteCreatedByUserId = "";

    public long remoteUpdatedAt;
    public int remoteVersion;

    public long conflictDetectedAt;
}
