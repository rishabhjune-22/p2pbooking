package com.example.roombooking.model.booking;

import com.google.gson.annotations.SerializedName;

public class RoomAvailabilityBookingItem {

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("room_id")
    private int roomId;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("selection_label")
    private String selectionLabel;

    @SerializedName("guest_name")
    private String guestName;

    @SerializedName("guest_gender")
    private String guestGender;

    @SerializedName("requestor_name")
    private String requestorName;

    @SerializedName("arrival_at")
    private String arrivalAt;

    @SerializedName("departure_at")
    private String departureAt;

    @SerializedName("status")
    private String status;

    public int getBookingId() {
        return bookingId;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getSelectionLabel() {
        return selectionLabel;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestGender() {
        return guestGender;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public String getArrivalAt() {
        return arrivalAt;
    }

    public String getDepartureAt() {
        return departureAt;
    }

    public String getStatus() {
        return status;
    }

    public String getSafeRoomName() {
        return roomName != null ? roomName : "";
    }

    public String getSafeSelectionLabel() {
        return selectionLabel != null && !selectionLabel.trim().isEmpty()
                ? selectionLabel
                : getSafeRoomName();
    }

    public String getSafeGuestName() {
        return guestName != null ? guestName : "";
    }

    public String getSafeRequestorName() {
        return requestorName != null ? requestorName : "";
    }

    public String getSafeStatus() {
        return status != null ? status : "";
    }
}
