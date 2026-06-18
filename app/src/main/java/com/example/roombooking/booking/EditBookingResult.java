package com.example.roombooking.booking;

final class EditBookingResult {

    private final int bookingId;
    private final String updatedStatus;
    private final String arrivalAt;
    private final String departureAt;

    EditBookingResult(
            int bookingId,
            String updatedStatus,
            String arrivalAt,
            String departureAt
    ) {
        this.bookingId = bookingId;
        this.updatedStatus = updatedStatus;
        this.arrivalAt = arrivalAt;
        this.departureAt = departureAt;
    }

    int getBookingId() {
        return bookingId;
    }

    String getUpdatedStatus() {
        return updatedStatus;
    }

    String getArrivalAt() {
        return arrivalAt;
    }

    String getDepartureAt() {
        return departureAt;
    }
}
