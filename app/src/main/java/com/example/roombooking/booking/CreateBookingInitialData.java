package com.example.roombooking.booking;

final class CreateBookingInitialData {

    private final Integer roomId;
    private final String roomName;
    private final String arrivalDate;
    private final String departureDate;
    private final boolean partialRoom;
    private final String availableFromDate;
    private final String availableFromTime;

    CreateBookingInitialData(
            Integer roomId,
            String roomName,
            String arrivalDate,
            String departureDate,
            boolean partialRoom,
            String availableFromDate,
            String availableFromTime
    ) {
        this.roomId = roomId;
        this.roomName = clean(roomName);
        this.arrivalDate = clean(arrivalDate);
        this.departureDate = clean(departureDate);
        this.partialRoom = partialRoom;
        this.availableFromDate = clean(availableFromDate);
        this.availableFromTime = clean(availableFromTime);
    }

    Integer getRoomId() {
        return roomId;
    }

    String getRoomName() {
        return roomName;
    }

    String getArrivalDate() {
        return arrivalDate;
    }

    String getDepartureDate() {
        return departureDate;
    }

    boolean isPartialRoom() {
        return partialRoom;
    }

    String getAvailableFromDate() {
        return availableFromDate;
    }

    String getAvailableFromTime() {
        return availableFromTime;
    }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }
}
