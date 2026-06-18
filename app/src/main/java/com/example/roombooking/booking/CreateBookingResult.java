package com.example.roombooking.booking;

final class CreateBookingResult {

    private final String message;

    CreateBookingResult(String message) {
        this.message = message;
    }

    String getMessage() {
        return message;
    }
}
