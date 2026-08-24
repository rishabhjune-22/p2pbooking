package com.example.roombooking.requester;

import com.google.gson.annotations.SerializedName;

public class BookingRequestItem {

    @SerializedName("id")
    private int id;

    @SerializedName("requester_name")
    private String requesterName;

    @SerializedName("requester_email")
    private String requesterEmail;

    @SerializedName("status")
    private String status;

    @SerializedName("requested_at")
    private String requestedAt;

    @SerializedName("reviewed_at")
    private String reviewedAt;

    @SerializedName("reviewed_by_name")
    private String reviewedByName;

    @SerializedName("admin_remarks")
    private String adminRemarks;

    @SerializedName("is_deleted")
    private boolean deleted;

    @SerializedName("approved_booking_id")
    private Integer approvedBookingId;

    @SerializedName("assigned_room_name")
    private String assignedRoomName;

    @SerializedName("arrival_at")
    private String arrivalAt;

    @SerializedName("departure_at")
    private String departureAt;

    @SerializedName("preferred_prefix")
    private String preferredPrefix;

    @SerializedName("preferred_room")
    private Integer preferredRoom;

    @SerializedName("preferred_room_name")
    private String preferredRoomName;

    @SerializedName("room_preference_note")
    private String roomPreferenceNote;

    @SerializedName("visitor_name")
    private String visitorName;

    @SerializedName("visitor_designation")
    private String visitorDesignation;

    @SerializedName("visitor_organisation")
    private String visitorOrganisation;

    @SerializedName("visitor_gender")
    private String visitorGender;

    @SerializedName("visitor_mobile")
    private String visitorMobile;

    @SerializedName("visitor_email")
    private String visitorEmail;

    @SerializedName("visitor_category")
    private String visitorCategory;

    @SerializedName("purpose_of_visit")
    private String purposeOfVisit;

    @SerializedName("budget_head_type")
    private String budgetHeadType;

    @SerializedName("budget_head_value")
    private String budgetHeadValue;

    @SerializedName("budget_head_name")
    private String budgetHeadName;

    @SerializedName("budget_head_department_name")
    private String budgetHeadDepartmentName;

    @SerializedName("budget_head_project_code")
    private String budgetHeadProjectCode;

    @SerializedName("attender_required")
    private boolean attenderRequired;

    @SerializedName("attender_general_shift")
    private boolean attenderGeneralShift;

    @SerializedName("attender_morning_shift")
    private boolean attenderMorningShift;

    @SerializedName("attender_day_shift")
    private boolean attenderDayShift;

    @SerializedName("requestor_name")
    private String requestorName;

    @SerializedName("requestor_designation")
    private String requestorDesignation;

    @SerializedName("requestor_department")
    private String requestorDepartment;

    @SerializedName("requestor_mobile")
    private String requestorMobile;

    @SerializedName("requestor_email")
    private String requestorEmail;

    public int getId() {
        return id;
    }

    public String getRequesterName() {
        return safe(requesterName);
    }

    public String getRequesterEmail() {
        return safe(requesterEmail);
    }

    public String getStatus() {
        return safe(status);
    }

    public String getRequestedAt() {
        return safe(requestedAt);
    }

    public String getReviewedAt() {
        return safe(reviewedAt);
    }

    public String getReviewedByName() {
        return safe(reviewedByName);
    }

    public String getAdminRemarks() {
        return safe(adminRemarks);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Integer getApprovedBookingId() {
        return approvedBookingId;
    }

    public String getAssignedRoomName() {
        return safe(assignedRoomName);
    }

    public String getArrivalAt() {
        return safe(arrivalAt);
    }

    public String getDepartureAt() {
        return safe(departureAt);
    }

    public String getPreferredPrefix() {
        return safe(preferredPrefix);
    }

    public Integer getPreferredRoom() {
        return preferredRoom;
    }

    public String getPreferredRoomName() {
        return safe(preferredRoomName);
    }

    public String getRoomPreferenceNote() {
        return safe(roomPreferenceNote);
    }

    public String getVisitorName() {
        return safe(visitorName);
    }

    public String getVisitorDesignation() {
        return safe(visitorDesignation);
    }

    public String getVisitorOrganisation() {
        return safe(visitorOrganisation);
    }

    public String getVisitorGender() {
        return safe(visitorGender);
    }

    public String getVisitorMobile() {
        return safe(visitorMobile);
    }

    public String getVisitorEmail() {
        return safe(visitorEmail);
    }

    public String getVisitorCategory() {
        return safe(visitorCategory);
    }

    public String getPurposeOfVisit() {
        return safe(purposeOfVisit);
    }

    public String getBudgetHeadType() {
        return safe(budgetHeadType);
    }

    public String getBudgetHeadValue() {
        return safe(budgetHeadValue);
    }

    public String getBudgetHeadName() {
        return safe(budgetHeadName);
    }

    public String getBudgetHeadDepartmentName() {
        return safe(budgetHeadDepartmentName);
    }

    public String getBudgetHeadProjectCode() {
        return safe(budgetHeadProjectCode);
    }

    public boolean isAttenderRequired() {
        return attenderRequired;
    }

    public boolean isAttenderGeneralShift() {
        return attenderGeneralShift;
    }

    public boolean isAttenderMorningShift() {
        return attenderMorningShift;
    }

    public boolean isAttenderDayShift() {
        return attenderDayShift;
    }

    public String getRequestorName() {
        return safe(requestorName);
    }

    public String getRequestorDesignation() {
        return safe(requestorDesignation);
    }

    public String getRequestorDepartment() {
        return safe(requestorDepartment);
    }

    public String getRequestorMobile() {
        return safe(requestorMobile);
    }

    public String getRequestorEmail() {
        return safe(requestorEmail);
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
