package com.example.p2proombooking;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "bookings",
        indices = {
                @Index("bookingId"),
                @Index("roomId"),
                @Index("updatedAt"),
                @Index("syncFlag"),
                @Index("status")
        }
)
public class BookingEntity {

    @PrimaryKey
    @NonNull
    public String bookingId = "";

    @NonNull
    public String roomId = "";

    public long startUtc;
    public long endUtc;

    @NonNull
    public String createdByUserId = "";

    @NonNull
    public String createdByDeviceId = "";

    public long createdAt;
    public long updatedAt;

    public int version = 1;

    public String canceledByUserId;   // nullable OK
    public long canceledAt;           // 0 unless canceled

    @NonNull
    public String status = BookingConstants.STATUS_ACTIVE;

    @NonNull
    public String syncFlag = BookingConstants.SYNC_PENDING;

    public long lastSyncedAt;         // 0 if never synced
    public int deletedFlag;           // 0 normal, 1 deleted (future)
}
