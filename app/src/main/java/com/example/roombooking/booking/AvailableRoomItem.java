package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class AvailableRoomItem {

    private static final String STATUS_PARTIAL = "partial";

    @SerializedName("room_id")
    private int roomId;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("selection_label")
    private String selectionLabel;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("availability_status")
    private String availabilityStatus;

    @SerializedName("available_from_date")
    private String availableFromDate;

    @SerializedName("available_from_time")
    private String availableFromTime;

    @SerializedName("available_from")
    private String availableFrom;

    public int getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getSelectionLabel() {
        return selectionLabel;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public String getAvailableFromDate() {
        return availableFromDate;
    }

    public String getAvailableFromTime() {
        return availableFromTime;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    public boolean isPartiallyAvailable() {
        return availabilityStatus != null
                && STATUS_PARTIAL.equalsIgnoreCase(availabilityStatus.trim());
    }

    public String getSafeRoomName() {
        return roomName != null ? roomName : "";
    }

    public String getSafeSelectionLabel() {
        return selectionLabel != null && !selectionLabel.trim().isEmpty()
                ? selectionLabel
                : getSafeRoomName();
    }

    public String getSafeAvailableFromDate() {
        return availableFromDate != null ? availableFromDate : "";
    }

    public String getSafeAvailableFromTime() {
        return availableFromTime != null ? availableFromTime : "";
    }

    public String getSafeAvailableFrom() {
        return availableFrom != null ? availableFrom : "";
    }
}
