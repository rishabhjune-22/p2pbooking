package com.example.roombooking.booking;

public class BookingCreateResponse {
    private String message;
    private int booking_id;
    private int room_id;
    private String room_name;
    private String visitor_name;

    public String getMessage() {
        return message;
    }

    public int getBooking_id() {
        return booking_id;
    }

    public int getRoom_id() {
        return room_id;
    }

    public String getRoom_name() {
        return room_name;
    }

    public String getVisitor_name() {
        return visitor_name;
    }
}