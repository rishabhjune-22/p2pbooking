package com.example.roombooking.booking;

final class EditBookingFormState {

    static final String FIELD_ROOM_CHARGES_AMOUNT = "room_charges_amount";
    static final String FIELD_ATTENDER_CHARGES_AMOUNT = "attender_charges_amount";
    static final String FIELD_BUDGET_HEAD_VALUE = "budget_head_value";
    static final String BUDGET_HEAD_INDIVIDUAL = "individual";
    static final String BUDGET_HEAD_INSTITUTE = "institute_head";
    static final String BUDGET_HEAD_PROJECT = "project_head";

    private Integer roomId;
    private String arrivalAt;
    private String departureAt;
    private long arrivalAtMillis;
    private long departureAtMillis;

    private String visitorName;
    private String visitorDesignation;
    private String visitorOrganisation;
    private String visitorGender;
    private String visitorAddress;
    private String visitorMobile;
    private String visitorEmail;
    private String purpose;
    private String visitorCategory;

    private boolean attenderRequired;
    private int attenderCountPerDay;
    private boolean attenderGeneralShift;
    private boolean attenderMorningShift;
    private boolean attenderDayShift;
    private String roomChargesStatus;
    private String attenderChargesStatus;
    private String roomChargesAmount;
    private String attenderChargesAmount;

    private String budgetHeadType;
    private String budgetHeadValue;
    private String budgetHeadName;
    private String budgetHeadDepartmentName;
    private String budgetHeadProjectCode;

    private String requestorName;
    private String requestorDesignation;
    private String requestorDepartment;
    private String requestorMobile;

    private String logisticsName;
    private String logisticsDesignation;
    private String logisticsMobile;

    EditBookingFormState copy() {
        EditBookingFormState copy = new EditBookingFormState();
        copy.roomId = roomId;
        copy.arrivalAt = arrivalAt;
        copy.departureAt = departureAt;
        copy.arrivalAtMillis = arrivalAtMillis;
        copy.departureAtMillis = departureAtMillis;
        copy.visitorName = visitorName;
        copy.visitorDesignation = visitorDesignation;
        copy.visitorOrganisation = visitorOrganisation;
        copy.visitorGender = visitorGender;
        copy.visitorAddress = visitorAddress;
        copy.visitorMobile = visitorMobile;
        copy.visitorEmail = visitorEmail;
        copy.purpose = purpose;
        copy.visitorCategory = visitorCategory;
        copy.attenderRequired = attenderRequired;
        copy.attenderCountPerDay = attenderCountPerDay;
        copy.attenderGeneralShift = attenderGeneralShift;
        copy.attenderMorningShift = attenderMorningShift;
        copy.attenderDayShift = attenderDayShift;
        copy.roomChargesStatus = roomChargesStatus;
        copy.attenderChargesStatus = attenderChargesStatus;
        copy.roomChargesAmount = roomChargesAmount;
        copy.attenderChargesAmount = attenderChargesAmount;
        copy.budgetHeadType = budgetHeadType;
        copy.budgetHeadValue = budgetHeadValue;
        copy.budgetHeadName = budgetHeadName;
        copy.budgetHeadDepartmentName = budgetHeadDepartmentName;
        copy.budgetHeadProjectCode = budgetHeadProjectCode;
        copy.requestorName = requestorName;
        copy.requestorDesignation = requestorDesignation;
        copy.requestorDepartment = requestorDepartment;
        copy.requestorMobile = requestorMobile;
        copy.logisticsName = logisticsName;
        copy.logisticsDesignation = logisticsDesignation;
        copy.logisticsMobile = logisticsMobile;
        return copy;
    }

    Integer getRoomId() { return roomId; }
    void setRoomId(Integer roomId) { this.roomId = roomId; }
    String getArrivalAt() { return arrivalAt; }
    void setArrivalAt(String arrivalAt) { this.arrivalAt = clean(arrivalAt); }
    String getDepartureAt() { return departureAt; }
    void setDepartureAt(String departureAt) { this.departureAt = clean(departureAt); }
    long getArrivalAtMillis() { return arrivalAtMillis; }
    void setArrivalAtMillis(long arrivalAtMillis) { this.arrivalAtMillis = arrivalAtMillis; }
    long getDepartureAtMillis() { return departureAtMillis; }
    void setDepartureAtMillis(long departureAtMillis) { this.departureAtMillis = departureAtMillis; }
    String getVisitorName() { return visitorName; }
    void setVisitorName(String visitorName) { this.visitorName = clean(visitorName); }
    String getVisitorDesignation() { return visitorDesignation; }
    void setVisitorDesignation(String visitorDesignation) { this.visitorDesignation = clean(visitorDesignation); }
    String getVisitorOrganisation() { return visitorOrganisation; }
    void setVisitorOrganisation(String visitorOrganisation) { this.visitorOrganisation = clean(visitorOrganisation); }
    String getVisitorGender() { return visitorGender; }
    void setVisitorGender(String visitorGender) { this.visitorGender = clean(visitorGender); }
    String getVisitorAddress() { return visitorAddress; }
    void setVisitorAddress(String visitorAddress) { this.visitorAddress = clean(visitorAddress); }
    String getVisitorMobile() { return visitorMobile; }
    void setVisitorMobile(String visitorMobile) { this.visitorMobile = clean(visitorMobile); }
    String getVisitorEmail() { return visitorEmail; }
    void setVisitorEmail(String visitorEmail) { this.visitorEmail = clean(visitorEmail); }
    String getPurpose() { return purpose; }
    void setPurpose(String purpose) { this.purpose = clean(purpose); }
    String getVisitorCategory() { return visitorCategory; }
    void setVisitorCategory(String visitorCategory) { this.visitorCategory = clean(visitorCategory); }
    boolean isAttenderRequired() { return attenderRequired; }
    void setAttenderRequired(boolean attenderRequired) { this.attenderRequired = attenderRequired; }
    int getAttenderCountPerDay() { return attenderCountPerDay; }
    void setAttenderCountPerDay(int attenderCountPerDay) { this.attenderCountPerDay = attenderCountPerDay; }
    boolean isAttenderGeneralShift() { return attenderGeneralShift; }
    void setAttenderGeneralShift(boolean attenderGeneralShift) { this.attenderGeneralShift = attenderGeneralShift; }
    boolean isAttenderMorningShift() { return attenderMorningShift; }
    void setAttenderMorningShift(boolean attenderMorningShift) { this.attenderMorningShift = attenderMorningShift; }
    boolean isAttenderDayShift() { return attenderDayShift; }
    void setAttenderDayShift(boolean attenderDayShift) { this.attenderDayShift = attenderDayShift; }
    String getRoomChargesStatus() { return roomChargesStatus; }
    void setRoomChargesStatus(String roomChargesStatus) { this.roomChargesStatus = clean(roomChargesStatus); }
    String getAttenderChargesStatus() { return attenderChargesStatus; }
    void setAttenderChargesStatus(String attenderChargesStatus) { this.attenderChargesStatus = clean(attenderChargesStatus); }
    String getRoomChargesAmount() { return roomChargesAmount; }
    void setRoomChargesAmount(String roomChargesAmount) { this.roomChargesAmount = clean(roomChargesAmount); }
    String getAttenderChargesAmount() { return attenderChargesAmount; }
    void setAttenderChargesAmount(String attenderChargesAmount) { this.attenderChargesAmount = clean(attenderChargesAmount); }
    String getBudgetHeadType() { return budgetHeadType != null ? budgetHeadType : ""; }
    void setBudgetHeadType(String budgetHeadType) { this.budgetHeadType = clean(budgetHeadType); }
    String getBudgetHeadValue() { return budgetHeadValue; }
    void setBudgetHeadValue(String budgetHeadValue) { this.budgetHeadValue = clean(budgetHeadValue); }
    String getBudgetHeadName() { return budgetHeadName; }
    void setBudgetHeadName(String budgetHeadName) { this.budgetHeadName = clean(budgetHeadName); }
    String getBudgetHeadDepartmentName() { return budgetHeadDepartmentName; }
    void setBudgetHeadDepartmentName(String budgetHeadDepartmentName) { this.budgetHeadDepartmentName = clean(budgetHeadDepartmentName); }
    String getBudgetHeadProjectCode() { return budgetHeadProjectCode; }
    void setBudgetHeadProjectCode(String budgetHeadProjectCode) { this.budgetHeadProjectCode = clean(budgetHeadProjectCode); }
    String getRequestorName() { return requestorName; }
    void setRequestorName(String requestorName) { this.requestorName = clean(requestorName); }
    String getRequestorDesignation() { return requestorDesignation; }
    void setRequestorDesignation(String requestorDesignation) { this.requestorDesignation = clean(requestorDesignation); }
    String getRequestorDepartment() { return requestorDepartment; }
    void setRequestorDepartment(String requestorDepartment) { this.requestorDepartment = clean(requestorDepartment); }
    String getRequestorMobile() { return requestorMobile; }
    void setRequestorMobile(String requestorMobile) { this.requestorMobile = clean(requestorMobile); }
    String getLogisticsName() { return logisticsName; }
    void setLogisticsName(String logisticsName) { this.logisticsName = clean(logisticsName); }
    String getLogisticsDesignation() { return logisticsDesignation; }
    void setLogisticsDesignation(String logisticsDesignation) { this.logisticsDesignation = clean(logisticsDesignation); }
    String getLogisticsMobile() { return logisticsMobile; }
    void setLogisticsMobile(String logisticsMobile) { this.logisticsMobile = clean(logisticsMobile); }

    private String clean(String value) {
        return value != null ? value.trim() : "";
    }

}
