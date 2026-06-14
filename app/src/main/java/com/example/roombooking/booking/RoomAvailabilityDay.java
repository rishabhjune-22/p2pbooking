package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class RoomAvailabilityDay {

    @SerializedName("date")
    private String date;

    @SerializedName("total_rooms")
    private int totalRooms;

    @SerializedName("booked_rooms")
    private int bookedRooms;

    @SerializedName("available_rooms")
    private int availableRooms;

    @SerializedName("has_before_6pm_booking")
    private boolean hasBefore6pmBooking;

    public String getDate() {
        return date;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public int getBookedRooms() {
        return bookedRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public boolean hasBefore6pmBooking() {
        return hasBefore6pmBooking;
    }

    public boolean hasSameContent(RoomAvailabilityDay other) {
        if (other == null) return false;

        return totalRooms == other.totalRooms
                && bookedRooms == other.bookedRooms
                && availableRooms == other.availableRooms
                && hasBefore6pmBooking == other.hasBefore6pmBooking
                && Objects.equals(date, other.date);
    }
}