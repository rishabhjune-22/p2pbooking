package com.example.p2proombooking;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "rooms",
        indices = {
                @Index(value = {"building", "number", "suffix"}, unique = true)
        }
)
public class RoomEntity {
    @PrimaryKey
    @NonNull
    public String roomId;        // e.g. DELTA_0101_A

    @NonNull
    public String building;      // DELTA / GAMMA / BETA

    public int number;           // 101, 102, 1001, 1104 etc.

    @NonNull
    public String suffix;        // A/B/C/D

    @NonNull
    public String displayName;   // "Delta 101A"
}