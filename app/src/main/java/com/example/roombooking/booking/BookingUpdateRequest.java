package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class BookingUpdateRequest {

    @SerializedName("room")
    private final Integer room;

    @SerializedName("arrival_at")
    private final String arrivalAt;

    @SerializedName("departure_at")
    private final String departureAt;

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

    @SerializedName("purpose_of_visit")
    private final String purposeOfVisit;

    @SerializedName("visitor_category")
    private final String visitorCategory;

    @SerializedName("attender_required")
    private final boolean attenderRequired;

    @SerializedName("attender_count_per_day")
    private final int attenderCountPerDay;

    @SerializedName("attender_general_shift")
    private final boolean attenderGeneralShift;

    @SerializedName("attender_morning_shift")
    private final boolean attenderMorningShift;

    @SerializedName("attender_day_shift")
    private final boolean attenderDayShift;

    @SerializedName("attender_night_shift")
    private final boolean attenderNightShift;

    @SerializedName("room_charges_status")
    private final String roomChargesStatus;

    @SerializedName("attender_charges_status")
    private final String attenderChargesStatus;

    @SerializedName("room_charges_amount")
    private final String roomChargesAmount;

    @SerializedName("attender_charges_amount")
    private final String attenderChargesAmount;

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

    public BookingUpdateRequest(
            Integer room,
            String arrivalAt,
            String departureAt,

            String visitorName,
            String visitorDesignation,
            String visitorOrganisation,
            String visitorGender,
            String visitorAddress,
            String visitorMobile,
            String visitorEmail,
            String purposeOfVisit,
            String visitorCategory,

            boolean attenderRequired,
            int attenderCountPerDay,
            boolean attenderGeneralShift,
            boolean attenderMorningShift,
            boolean attenderDayShift,
            boolean attenderNightShift,
            String roomChargesStatus,
            String attenderChargesStatus,
            String roomChargesAmount,
            String attenderChargesAmount,

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

        this.visitorName = visitorName;
        this.visitorDesignation = visitorDesignation;
        this.visitorOrganisation = visitorOrganisation;
        this.visitorGender = visitorGender;
        this.visitorAddress = visitorAddress;
        this.visitorMobile = visitorMobile;
        this.visitorEmail = visitorEmail;
        this.purposeOfVisit = purposeOfVisit;
        this.visitorCategory = visitorCategory;

        this.attenderRequired = attenderRequired;
        this.attenderCountPerDay = attenderCountPerDay;
        this.attenderGeneralShift = attenderGeneralShift;
        this.attenderMorningShift = attenderMorningShift;
        this.attenderDayShift = attenderDayShift;
        this.attenderNightShift = attenderNightShift;
        this.roomChargesStatus = roomChargesStatus;
        this.attenderChargesStatus = attenderChargesStatus;
        this.roomChargesAmount = roomChargesAmount;
        this.attenderChargesAmount = attenderChargesAmount;

        this.requesteeName = requesteeName;
        this.requesteeDesignation = requesteeDesignation;
        this.requesteeDepartment = requesteeDepartment;
        this.requesteeMobile = requesteeMobile;

        this.logisticsName = logisticsName;
        this.logisticsDesignation = logisticsDesignation;
        this.logisticsMobile = logisticsMobile;
    }
}
