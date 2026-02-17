package com.example.p2proombooking;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "bookings",
        indices = {
                @Index(value = {"roomId", "startUtc", "endUtc"})
        }
)
public class BookingEntity {
    @PrimaryKey
    @NonNull
    public String bookingId;          // UUID

    @NonNull
    public String roomId;

    public long startUtc;             // epoch millis UTC
    public long endUtc;               // epoch millis UTC

    @NonNull
    public String createdByUserId;

    public long createdAt;
    public long lastModifiedAt;

    @NonNull
    public String status;// PENDING/ACTIVE/CONFLICTED/CANCELED (for now keep "ACTIVE")

    public int version;      // start from 1
    public long updatedAt;   // last modified time (epoch millis)
}