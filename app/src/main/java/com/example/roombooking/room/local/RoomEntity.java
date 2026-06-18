package com.example.roombooking.room.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "rooms",
        indices = {
                @Index("prefix"),
                @Index("number"),
                @Index(value = {"prefix", "number"})
        }
)
public class RoomEntity {

    @PrimaryKey
    private int id;

    @NonNull
    private String prefix;

    @NonNull
    private String number;

    @NonNull
    private String roomName;

    @NonNull
    private String hostelName;

    private boolean hasAttachedBath;

    @NonNull
    private String roomType;

    @NonNull
    private String selectionLabel;

    private int displayOrder;

    public RoomEntity(
            int id,
            @NonNull String prefix,
            @NonNull String number,
            @NonNull String roomName,
            @NonNull String hostelName,
            boolean hasAttachedBath,
            @NonNull String roomType,
            @NonNull String selectionLabel,
            int displayOrder
    ) {
        this.id = id;
        this.prefix = prefix;
        this.number = number;
        this.roomName = roomName;
        this.hostelName = hostelName;
        this.hasAttachedBath = hasAttachedBath;
        this.roomType = roomType;
        this.selectionLabel = selectionLabel;
        this.displayOrder = displayOrder;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getPrefix() {
        return prefix;
    }

    @NonNull
    public String getNumber() {
        return number;
    }

    @NonNull
    public String getRoomName() {
        return roomName;
    }

    @NonNull public String getHostelName() { return hostelName; }
    public boolean isHasAttachedBath() { return hasAttachedBath; }
    @NonNull public String getRoomType() { return roomType; }
    @NonNull public String getSelectionLabel() { return selectionLabel; }
    public int getDisplayOrder() { return displayOrder; }

    public void setId(int id) {
        this.id = id;
    }

    public void setPrefix(@NonNull String prefix) {
        this.prefix = prefix;
    }

    public void setNumber(@NonNull String number) {
        this.number = number;
    }

    public void setRoomName(@NonNull String roomName) {
        this.roomName = roomName;
    }

    public void setHostelName(@NonNull String hostelName) { this.hostelName = hostelName; }
    public void setHasAttachedBath(boolean value) { this.hasAttachedBath = value; }
    public void setRoomType(@NonNull String roomType) { this.roomType = roomType; }
    public void setSelectionLabel(@NonNull String label) { this.selectionLabel = label; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
