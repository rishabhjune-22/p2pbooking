package com.example.roombooking.booking;

final class EditBookingValidationResult {

    private final boolean valid;
    private final String message;
    private final String field;

    private EditBookingValidationResult(boolean valid, String message, String field) {
        this.valid = valid;
        this.message = message;
        this.field = field;
    }

    static EditBookingValidationResult valid() {
        return new EditBookingValidationResult(true, "", "");
    }

    static EditBookingValidationResult invalid(String message) {
        return new EditBookingValidationResult(false, message, "");
    }

    static EditBookingValidationResult invalid(String message, String field) {
        return new EditBookingValidationResult(false, message, field);
    }

    boolean isValid() {
        return valid;
    }

    String getMessage() {
        return message;
    }

    String getField() {
        return field;
    }
}
