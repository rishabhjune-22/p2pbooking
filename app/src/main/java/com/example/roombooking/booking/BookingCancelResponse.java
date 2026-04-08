package com.example.roombooking.booking;

public class BookingCancelResponse {
    private String message;
    private int booking_id;
    private String status;

    public String getMessage() {
        return message;
    }

    public int getBooking_id() {
        return booking_id;
    }

    public String getStatus() {
        return status;
    }
}