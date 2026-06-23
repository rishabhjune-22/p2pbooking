package com.example.roombooking.admin;

import com.google.gson.annotations.SerializedName;

public class BookingRequestDecisionRequest {

    @SerializedName("room")
    private final Integer room;

    @SerializedName("remarks")
    private final String remarks;

    public BookingRequestDecisionRequest(Integer room, String remarks) {
        this.room = room;
        this.remarks = remarks;
    }
}
