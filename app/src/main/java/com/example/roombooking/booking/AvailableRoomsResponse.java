package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AvailableRoomsResponse {

    @SerializedName("date")
    private String date;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("total_available_rooms")
    private int totalAvailableRooms;

    @SerializedName("rooms")
    private List<AvailableRoomItem> rooms;

    public String getDate() {
        return date;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getTotalAvailableRooms() {
        return totalAvailableRooms;
    }

    public List<AvailableRoomItem> getRooms() {
        return rooms;
    }

    public boolean hasRooms() {
        return rooms != null && !rooms.isEmpty();
    }
}