package com.example.roombooking.booking;

final class CreateBookingResult {

    private final String message;
    private final String status;

    CreateBookingResult(String message, String status) {
        this.message = message;
        this.status = status;
    }

    String getMessage() {
        return message;
    }

    String getStatus() {
        return status;
    }
}
