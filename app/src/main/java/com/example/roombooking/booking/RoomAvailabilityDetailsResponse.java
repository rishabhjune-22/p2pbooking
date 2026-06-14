package com.example.roombooking.booking;

import com.example.roombooking.model.booking.RoomAvailabilityBookingItem;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RoomAvailabilityDetailsResponse {

    @SerializedName("date")
    private String date;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("total_bookings")
    private int totalBookings;

    @SerializedName("bookings")
    private List<RoomAvailabilityBookingItem> bookings;

    public String getDate() {
        return date;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public List<RoomAvailabilityBookingItem> getBookings() {
        return bookings;
    }

    public boolean hasBookings() {
        return bookings != null && !bookings.isEmpty();
    }
}