package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;

public class BookingCreateRequest {

    @SerializedName("room")
    private final Integer room;

    @SerializedName("arrival_at")
    private final String arrivalAt;

    @SerializedName("departure_at")
    private final String departureAt;

    @SerializedName("created_by_name")
    private final String createdByName;

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

    @SerializedName("room_charges_status")
    private final String roomChargesStatus;

    @SerializedName("attender_charges_status")
    private final String attenderChargesStatus;

    @SerializedName("room_charges_amount")
    private final String roomChargesAmount;

    @SerializedName("attender_charges_amount")
    private final String attenderChargesAmount;

    @SerializedName("budget_head_type")
    private final String budgetHeadType;

    @SerializedName("budget_head_value")
    private final String budgetHeadValue;

    @SerializedName("requestor_name")
    private final String requestorName;

    @SerializedName("requestor_designation")
    private final String requestorDesignation;

    @SerializedName("requestor_department")
    private final String requestorDepartment;

    @SerializedName("requestor_mobile")
    private final String requestorMobile;

    @SerializedName("logistics_name")
    private final String logisticsName;

    @SerializedName("logistics_designation")
    private final String logisticsDesignation;

    @SerializedName("logistics_mobile")
    private final String logisticsMobile;

    public BookingCreateRequest(
            Integer room,
            String arrivalAt,
            String departureAt,
            String createdByName,

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
            String roomChargesStatus,
            String attenderChargesStatus,
            String roomChargesAmount,
            String attenderChargesAmount,

            String budgetHeadType,
            String budgetHeadValue,

            String requestorName,
            String requestorDesignation,
            String requestorDepartment,
            String requestorMobile,

            String logisticsName,
            String logisticsDesignation,
            String logisticsMobile
    ) {
        this.room = room;
        this.arrivalAt = arrivalAt;
        this.departureAt = departureAt;
        this.createdByName = createdByName;

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
        this.roomChargesStatus = roomChargesStatus;
        this.attenderChargesStatus = attenderChargesStatus;
        this.roomChargesAmount = roomChargesAmount;
        this.attenderChargesAmount = attenderChargesAmount;
        this.budgetHeadType = budgetHeadType;
        this.budgetHeadValue = budgetHeadValue;

        this.requestorName = requestorName;
        this.requestorDesignation = requestorDesignation;
        this.requestorDepartment = requestorDepartment;
        this.requestorMobile = requestorMobile;

        this.logisticsName = logisticsName;
        this.logisticsDesignation = logisticsDesignation;
        this.logisticsMobile = logisticsMobile;
    }
}
