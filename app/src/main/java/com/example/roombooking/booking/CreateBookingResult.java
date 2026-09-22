package com.example.roombooking.booking;

import com.example.roombooking.model.booking.BookingMailTemplate;

final class CreateBookingResult {

    private final String message;
    private final String status;
    private final BookingMailTemplate mailTemplate;

    CreateBookingResult(String message, String status) {
        this(message, status, null);
    }

    CreateBookingResult(String message, String status, BookingMailTemplate mailTemplate) {
        this.message = message;
        this.status = status;
        this.mailTemplate = mailTemplate;
    }

    String getMessage() {
        return message;
    }

    String getStatus() {
        return status;
    }

    BookingMailTemplate getMailTemplate() {
        return mailTemplate;
    }
}
