package com.example.roombooking.model.booking;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class BookingActionData {

    @SerializedName("booking_id")
    private int bookingId;

    @SerializedName("room_id")
    @Nullable
    private Integer roomId;

    @SerializedName("room_name")
    @Nullable
    private String roomName;

    @SerializedName("visitor_name")
    @Nullable
    private String visitorName;

    @SerializedName("status")
    @Nullable
    private String status;

    @SerializedName("mail_template")
    @Nullable
    private BookingMailTemplate mailTemplate;

    public int getBookingId() {
        return bookingId;
    }

    @Nullable
    public Integer getRoomId() {
        return roomId;
    }

    @Nullable
    public String getRoomName() {
        return roomName;
    }

    @Nullable
    public String getVisitorName() {
        return visitorName;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    public String getSafeRoomName() {
        return roomName != null ? roomName : "";
    }

    public String getSafeVisitorName() {
        return visitorName != null ? visitorName : "";
    }

    public String getSafeStatus() {
        return status != null ? status : "";
    }

    @Nullable
    public BookingMailTemplate getMailTemplate() {
        return mailTemplate;
    }
}
