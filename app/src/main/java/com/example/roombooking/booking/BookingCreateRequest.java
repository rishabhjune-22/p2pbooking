package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class BookingCreateRequest {

    @SerializedName("visitor_name")
    private final String visitorName;

    @SerializedName("visitor_designation")
    private final String visitorDesignation;

    @SerializedName("visitor_organisation")
    private final String visitorOrganisation;

    @SerializedName("visitor_gender")
    private final String visitorGender;

    @SerializedName("visitor_address")
    private final String visitorAddress;

    @SerializedName("visitor_mobile")
    private final String visitorMobile;

    @SerializedName("visitor_email")
    private final String visitorEmail;

    @SerializedName("arrival_date")
    private final String arrivalDate;

    @SerializedName("arrival_time")
    private final String arrivalTime;

    @SerializedName("departure_date")
    private final String departureDate;

    @SerializedName("departure_time")
    private final String departureTime;

    @SerializedName("purpose_of_visit")
    private final String purposeOfVisit;

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

    @SerializedName("room")
    private final int room;

    public BookingCreateRequest(
            String visitorName,
            String visitorDesignation,
            String visitorOrganisation,
            String visitorGender,
            String visitorAddress,
            String visitorMobile,
            String visitorEmail,
            String arrivalDate,
            String arrivalTime,
            String departureDate,
            String departureTime,
            String purposeOfVisit,
            String requesteeName,
            String requesteeDesignation,
            String requesteeDepartment,
            String requesteeMobile,
            String logisticsName,
            String logisticsDesignation,
            String logisticsMobile,
            int room
    ) {
        this.visitorName = visitorName;
        this.visitorDesignation = visitorDesignation;
        this.visitorOrganisation = visitorOrganisation;
        this.visitorGender = visitorGender;
        this.visitorAddress = visitorAddress;
        this.visitorMobile = visitorMobile;
        this.visitorEmail = visitorEmail;
        this.arrivalDate = arrivalDate;
        this.arrivalTime = arrivalTime;
        this.departureDate = departureDate;
        this.departureTime = departureTime;
        this.purposeOfVisit = purposeOfVisit;
        this.requesteeName = requesteeName;
        this.requesteeDesignation = requesteeDesignation;
        this.requesteeDepartment = requesteeDepartment;
        this.requesteeMobile = requesteeMobile;
        this.logisticsName = logisticsName;
        this.logisticsDesignation = logisticsDesignation;
        this.logisticsMobile = logisticsMobile;
        this.room = room;
    }
}