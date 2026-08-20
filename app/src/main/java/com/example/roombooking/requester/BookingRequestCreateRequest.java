package com.example.roombooking.requester;

import com.google.gson.annotations.SerializedName;

public class BookingRequestCreateRequest {

    @SerializedName("arrival_at")
    private final String arrivalAt;

    @SerializedName("departure_at")
    private final String departureAt;

    @SerializedName("preferred_prefix")
    private final String preferredPrefix;

    @SerializedName("preferred_room")
    private final Integer preferredRoom;

    @SerializedName("room_preference_note")
    private final String roomPreferenceNote;

    @SerializedName("visitor_name")
    private final String visitorName;

    @SerializedName("visitor_designation")
    private final String visitorDesignation;

    @SerializedName("visitor_organisation")
    private final String visitorOrganisation;

    @SerializedName("visitor_gender")
    private final String visitorGender;

    @SerializedName("visitor_mobile")
    private final String visitorMobile;

    @SerializedName("visitor_email")
    private final String visitorEmail;

    @SerializedName("visitor_category")
    private final String visitorCategory;

    @SerializedName("purpose_of_visit")
    private final String purposeOfVisit;

    @SerializedName("budget_head_type")
    private final String budgetHeadType;

    @SerializedName("budget_head_value")
    private final String budgetHeadValue;

    @SerializedName("budget_head_name")
    private final String budgetHeadName;

    @SerializedName("budget_head_department_name")
    private final String budgetHeadDepartmentName;

    @SerializedName("budget_head_project_code")
    private final String budgetHeadProjectCode;

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

    @SerializedName("requestor_name")
    private final String requestorName;

    @SerializedName("requestor_designation")
    private final String requestorDesignation;

    @SerializedName("requestor_department")
    private final String requestorDepartment;

    @SerializedName("requestor_mobile")
    private final String requestorMobile;

    @SerializedName("requestor_email")
    private final String requestorEmail;

    public BookingRequestCreateRequest(
            String arrivalAt,
            String departureAt,
            String preferredPrefix,
            Integer preferredRoom,
            String roomPreferenceNote,
            String visitorName,
            String visitorDesignation,
            String visitorOrganisation,
            String visitorGender,
            String visitorMobile,
            String visitorEmail,
            String visitorCategory,
            String purposeOfVisit,
            String budgetHeadType,
            String budgetHeadValue,
            String budgetHeadName,
            String budgetHeadDepartmentName,
            String budgetHeadProjectCode,
            boolean attenderRequired,
            int attenderCountPerDay,
            boolean attenderGeneralShift,
            boolean attenderMorningShift,
            boolean attenderDayShift,
            String requestorName,
            String requestorDesignation,
            String requestorDepartment,
            String requestorMobile,
            String requestorEmail
    ) {
        this.arrivalAt = arrivalAt;
        this.departureAt = departureAt;
        this.preferredPrefix = preferredPrefix;
        this.preferredRoom = preferredRoom;
        this.roomPreferenceNote = roomPreferenceNote;
        this.visitorName = visitorName;
        this.visitorDesignation = visitorDesignation;
        this.visitorOrganisation = visitorOrganisation;
        this.visitorGender = visitorGender;
        this.visitorMobile = visitorMobile;
        this.visitorEmail = visitorEmail;
        this.visitorCategory = visitorCategory;
        this.purposeOfVisit = purposeOfVisit;
        this.budgetHeadType = budgetHeadType;
        this.budgetHeadValue = budgetHeadValue;
        this.budgetHeadName = budgetHeadName;
        this.budgetHeadDepartmentName = budgetHeadDepartmentName;
        this.budgetHeadProjectCode = budgetHeadProjectCode;
        this.attenderRequired = attenderRequired;
        this.attenderCountPerDay = attenderCountPerDay;
        this.attenderGeneralShift = attenderGeneralShift;
        this.attenderMorningShift = attenderMorningShift;
        this.attenderDayShift = attenderDayShift;
        this.requestorName = requestorName;
        this.requestorDesignation = requestorDesignation;
        this.requestorDepartment = requestorDepartment;
        this.requestorMobile = requestorMobile;
        this.requestorEmail = requestorEmail;
    }
}
