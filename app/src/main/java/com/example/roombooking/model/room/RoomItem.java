package com.example.roombooking.model.room;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class RoomItem {

    @SerializedName("id")
    private int id;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("number")
    private String number;

    @SerializedName("room_name")
    private String roomName;

    public RoomItem() {
        // Required for Gson/Retrofit.
    }

    public RoomItem(
            int id,
            String prefix,
            String number,
            String roomName
    ) {
        this.id = id;
        this.prefix = prefix;
        this.number = number;
        this.roomName = roomName;
    }

    public int getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNumber() {
        return number;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getSafePrefix() {
        return prefix != null ? prefix : "";
    }

    public String getSafeNumber() {
        return number != null ? number : "";
    }

    public String getSafeRoomName() {
        return roomName != null ? roomName : "";
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public boolean hasSameContent(RoomItem other) {
        if (other == null) return false;

        return id == other.id
                && Objects.equals(prefix, other.prefix)
                && Objects.equals(number, other.number)
                && Objects.equals(roomName, other.roomName);
    }

    @Override
    public String toString() {
        return getSafeRoomName();
    }
}