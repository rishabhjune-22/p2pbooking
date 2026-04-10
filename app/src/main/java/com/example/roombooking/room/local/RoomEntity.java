package com.example.roombooking.room.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "rooms")
public class RoomEntity {

    @PrimaryKey
    private int id;

    @NonNull
    private String prefix;

    @NonNull
    private String number;

    @NonNull
    private String roomName;

    public RoomEntity(int id, @NonNull String prefix, @NonNull String number, @NonNull String roomName) {
        this.id = id;
        this.prefix = prefix;
        this.number = number;
        this.roomName = roomName;
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
}