package com.example.p2proombooking;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "booking_conflicts")
public class BookingConflictEntity {

    @PrimaryKey
    @NonNull
    public String bookingId;

    // Local snapshot
    public String localRoomId;
    public long localStartUtc;
    public long localEndUtc;
    public String localStatus;
    public int localVersion;
    public long localUpdatedAt;

    // Remote snapshot
    public String remoteRoomId;
    public long remoteStartUtc;
    public long remoteEndUtc;
    public String remoteStatus;
    public int remoteVersion;
    public long remoteUpdatedAt;

    public long detectedAt;
}
