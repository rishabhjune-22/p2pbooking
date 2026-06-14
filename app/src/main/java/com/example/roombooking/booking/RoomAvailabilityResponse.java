package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoomAvailabilityResponse {

    @SerializedName("month")
    private int month;

    @SerializedName("year")
    private int year;

    @SerializedName("groups")
    private List<RoomAvailabilityGroup> groups;

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public List<RoomAvailabilityGroup> getGroups() {
        return groups;
    }

    public boolean hasGroups() {
        return groups != null && !groups.isEmpty();
    }
}