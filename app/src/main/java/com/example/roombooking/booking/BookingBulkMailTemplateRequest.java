package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BookingBulkMailTemplateRequest {

    @SerializedName("booking_ids")
    private final List<Integer> bookingIds;

    public BookingBulkMailTemplateRequest(List<Integer> bookingIds) {
        this.bookingIds = bookingIds;
    }
}
