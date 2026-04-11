package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class EncryptedBookingRequest {

    @SerializedName("room")
    private final Integer room;

    @SerializedName("arrival_at")
    private final String arrivalAt;

    @SerializedName("departure_at")
    private final String departureAt;

    @SerializedName("encrypted_payload")
    private final String encryptedPayload;

    @SerializedName("payload_nonce")
    private final String payloadNonce;

    @SerializedName("payload_version")
    private final int payloadVersion;

    @SerializedName("requestee_name")
    private final String requesteeName;

    @SerializedName("requestee_designation")
    private final String requesteeDesignation;

    @SerializedName("requestee_department")
    private final String requesteeDepartment;

    @SerializedName("requestee_mobile")
    private final String requesteeMobile;

    @SerializedName("logistics_name")
    private final String logisticsName;

    @SerializedName("logistics_designation")
    private final String logisticsDesignation;

    @SerializedName("logistics_mobile")
    private final String logisticsMobile;

    public EncryptedBookingRequest(
            Integer room,
            String arrivalAt,
            String departureAt,
            String encryptedPayload,
            String payloadNonce,
            int payloadVersion,
            String requesteeName,
            String requesteeDesignation,
            String requesteeDepartment,
            String requesteeMobile,
            String logisticsName,
            String logisticsDesignation,
            String logisticsMobile
    ) {
        this.room = room;
        this.arrivalAt = arrivalAt;
        this.departureAt = departureAt;
        this.encryptedPayload = encryptedPayload;
        this.payloadNonce = payloadNonce;
        this.payloadVersion = payloadVersion;
        this.requesteeName = requesteeName;
        this.requesteeDesignation = requesteeDesignation;
        this.requesteeDepartment = requesteeDepartment;
        this.requesteeMobile = requesteeMobile;
        this.logisticsName = logisticsName;
        this.logisticsDesignation = logisticsDesignation;
        this.logisticsMobile = logisticsMobile;
    }
}