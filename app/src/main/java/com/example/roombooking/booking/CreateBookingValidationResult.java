package com.example.roombooking.booking;

final class CreateBookingValidationResult {

    private final boolean valid;
    private final String message;
    private final String field;

    private CreateBookingValidationResult(boolean valid, String message, String field) {
        this.valid = valid;
        this.message = message;
        this.field = field;
    }

    static CreateBookingValidationResult valid() {
        return new CreateBookingValidationResult(true, "", "");
    }

    static CreateBookingValidationResult invalid(String message) {
        return new CreateBookingValidationResult(false, message, "");
    }

    static CreateBookingValidationResult invalid(String message, String field) {
        return new CreateBookingValidationResult(false, message, field);
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
