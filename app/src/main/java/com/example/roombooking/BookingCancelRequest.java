package com.example.roombooking;

public class BookingCancelRequest {
    private String cancellation_reason;

    public BookingCancelRequest(String cancellation_reason) {
        this.cancellation_reason = cancellation_reason;
    }

    public String getCancellation_reason() {
        return cancellation_reason;
    }
}