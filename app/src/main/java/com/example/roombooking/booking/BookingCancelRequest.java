package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class BookingCancelRequest {

    @SerializedName("cancellation_reason")
    private final String cancellationReason;

    public BookingCancelRequest(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}