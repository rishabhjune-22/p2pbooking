package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoomAvailabilityGroup {

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("total_rooms")
    private int totalRooms;

    @SerializedName("calendar")
    private List<RoomAvailabilityDay> calendar;

    public String getPrefix() {
        return prefix;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public List<RoomAvailabilityDay> getCalendar() {
        return calendar;
    }

    public boolean hasCalendar() {
        return calendar != null && !calendar.isEmpty();
    }

    public boolean matchesPrefix(String selectedPrefix) {
        return prefix != null
                && selectedPrefix != null
                && prefix.equalsIgnoreCase(selectedPrefix);
    }
}