package com.example.roombooking.model.room;

import com.google.gson.annotations.SerializedName;

public class RoomItem {

    @SerializedName("id")
    private int id;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("number")
    private String number;

    @SerializedName("room_name")
    private String roomName;

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
}