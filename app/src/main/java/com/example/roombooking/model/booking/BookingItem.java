package com.example.roombooking.model.booking;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BookingItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("room")
    private int room;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("arrival_at")
    private String arrivalAt;

    @SerializedName("departure_at")
    private String departureAt;

    @SerializedName("created_by_name")
    private String createdByName;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("edit_history")
    private List<BookingEditHistoryItem> editHistory;

    @SerializedName("visitor_name")
    private String visitorName;

    @SerializedName("visitor_designation")
    private String visitorDesignation;

    @SerializedName("visitor_organisation")
    private String visitorOrganisation;

    @SerializedName("visitor_gender")
    private String visitorGender;

    @SerializedName("visitor_address")
    private String visitorAddress;

    @SerializedName("visitor_mobile")
    private String visitorMobile;

    @SerializedName("visitor_email")
    private String visitorEmail;

    @SerializedName("purpose_of_visit")
    private String purposeOfVisit;

    @SerializedName("visitor_category")
    private String visitorCategory;

    @SerializedName("attender_required")
    private boolean attenderRequired;

    @SerializedName("attender_count_per_day")
    private int attenderCountPerDay;

    @SerializedName("attender_general_shift")
    private boolean attenderGeneralShift;

    @SerializedName("attender_morning_shift")
    private boolean attenderMorningShift;

    @SerializedName("attender_day_shift")
    private boolean attenderDayShift;

    @SerializedName("room_charges_status")
    private String roomChargesStatus;

    @SerializedName("attender_charges_status")
    private String attenderChargesStatus;

    @SerializedName("room_charges_amount")
    private String roomChargesAmount;

    @SerializedName("attender_charges_amount")
    private String attenderChargesAmount;

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

    @SerializedName("requestor_name")
    private String requestorName;

    @SerializedName("requestor_designation")
    private String requestorDesignation;

    @SerializedName("requestor_department")
    private String requestorDepartment;

    @SerializedName("requestor_mobile")
    private String requestorMobile;

    @SerializedName("logistics_name")
    private String logisticsName;

    @SerializedName("logistics_designation")
    private String logisticsDesignation;

    @SerializedName("logistics_mobile")
    private String logisticsMobile;

    @SerializedName("status")
    private String status;

    public BookingItem() {
        // Required for Gson/Retrofit deserialization.
    }

    protected BookingItem(Parcel in) {
        id = in.readInt();
        room = in.readInt();
        roomName = in.readString();
        arrivalAt = in.readString();
        departureAt = in.readString();
        createdByName = in.readString();
        createdAt = in.readString();
        editHistory = in.createTypedArrayList(BookingEditHistoryItem.CREATOR);

        visitorName = in.readString();
        visitorDesignation = in.readString();
        visitorOrganisation = in.readString();
        visitorGender = in.readString();
        visitorAddress = in.readString();
        visitorMobile = in.readString();
        visitorEmail = in.readString();
        purposeOfVisit = in.readString();
        visitorCategory = in.readString();

        attenderRequired = in.readByte() != 0;
        attenderCountPerDay = in.readInt();
        attenderGeneralShift = in.readByte() != 0;
        attenderMorningShift = in.readByte() != 0;
        attenderDayShift = in.readByte() != 0;
        roomChargesStatus = in.readString();
        attenderChargesStatus = in.readString();
        roomChargesAmount = in.readString();
        attenderChargesAmount = in.readString();

        budgetHeadType = in.readString();
        budgetHeadValue = in.readString();
        budgetHeadName = in.readString();
        budgetHeadDepartmentName = in.readString();
        budgetHeadProjectCode = in.readString();

        requestorName = in.readString();
        requestorDesignation = in.readString();
        requestorDepartment = in.readString();
        requestorMobile = in.readString();

        logisticsName = in.readString();
        logisticsDesignation = in.readString();
        logisticsMobile = in.readString();

        status = in.readString();
    }

    public static final Creator<BookingItem> CREATOR = new Creator<BookingItem>() {
        @Override
        public BookingItem createFromParcel(Parcel in) {
            return new BookingItem(in);
        }

        @Override
        public BookingItem[] newArray(int size) {
            return new BookingItem[size];
        }
    };

    public boolean hasSameContent(BookingItem other) {
        if (other == null) return false;

        return id == other.id
                && room == other.room
                && attenderRequired == other.attenderRequired
                && attenderCountPerDay == other.attenderCountPerDay
                && attenderGeneralShift == other.attenderGeneralShift
                && attenderMorningShift == other.attenderMorningShift
                && attenderDayShift == other.attenderDayShift
                && Objects.equals(roomName, other.roomName)
                && Objects.equals(arrivalAt, other.arrivalAt)
                && Objects.equals(departureAt, other.departureAt)
                && Objects.equals(createdByName, other.createdByName)
                && Objects.equals(createdAt, other.createdAt)
                && Objects.equals(editHistory, other.editHistory)
                && Objects.equals(visitorName, other.visitorName)
                && Objects.equals(visitorDesignation, other.visitorDesignation)
                && Objects.equals(visitorOrganisation, other.visitorOrganisation)
                && Objects.equals(visitorGender, other.visitorGender)
                && Objects.equals(visitorAddress, other.visitorAddress)
                && Objects.equals(visitorMobile, other.visitorMobile)
                && Objects.equals(visitorEmail, other.visitorEmail)
                && Objects.equals(purposeOfVisit, other.purposeOfVisit)
                && Objects.equals(visitorCategory, other.visitorCategory)
                && Objects.equals(roomChargesStatus, other.roomChargesStatus)
                && Objects.equals(attenderChargesStatus, other.attenderChargesStatus)
                && Objects.equals(roomChargesAmount, other.roomChargesAmount)
                && Objects.equals(attenderChargesAmount, other.attenderChargesAmount)
                && Objects.equals(budgetHeadType, other.budgetHeadType)
                && Objects.equals(budgetHeadValue, other.budgetHeadValue)
                && Objects.equals(budgetHeadName, other.budgetHeadName)
                && Objects.equals(budgetHeadDepartmentName, other.budgetHeadDepartmentName)
                && Objects.equals(budgetHeadProjectCode, other.budgetHeadProjectCode)
                && Objects.equals(requestorName, other.requestorName)
                && Objects.equals(requestorDesignation, other.requestorDesignation)
                && Objects.equals(requestorDepartment, other.requestorDepartment)
                && Objects.equals(requestorMobile, other.requestorMobile)
                && Objects.equals(logisticsName, other.logisticsName)
                && Objects.equals(logisticsDesignation, other.logisticsDesignation)
                && Objects.equals(logisticsMobile, other.logisticsMobile)
                && Objects.equals(status, other.status);
    }

    public int getId() {
        return id;
    }

    public int getRoom() {
        return room;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getArrivalAt() {
        return arrivalAt;
    }

    public String getDepartureAt() {
        return departureAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<BookingEditHistoryItem> getEditHistory() {
        return editHistory == null ? Collections.emptyList() : editHistory;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public String getVisitorDesignation() {
        return visitorDesignation;
    }

    public String getVisitorOrganisation() {
        return visitorOrganisation;
    }

    public String getVisitorGender() {
        return visitorGender;
    }

    public String getVisitorAddress() {
        return visitorAddress;
    }

    public String getVisitorMobile() {
        return visitorMobile;
    }

    public String getVisitorEmail() {
        return visitorEmail;
    }

    public String getPurposeOfVisit() {
        return purposeOfVisit;
    }

    public String getVisitorCategory() {
        return visitorCategory;
    }

    public boolean isAttenderRequired() {
        return attenderRequired;
    }

    public int getAttenderCountPerDay() {
        return attenderCountPerDay;
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

    public String getRoomChargesStatus() {
        return roomChargesStatus;
    }

    public String getAttenderChargesStatus() {
        return attenderChargesStatus;
    }

    public String getRoomChargesAmount() {
        return roomChargesAmount;
    }

    public String getAttenderChargesAmount() {
        return attenderChargesAmount;
    }

    public String getBudgetHeadType() {
        return budgetHeadType;
    }

    public String getBudgetHeadValue() {
        return budgetHeadValue;
    }

    public String getBudgetHeadName() {
        return budgetHeadName;
    }

    public String getBudgetHeadDepartmentName() {
        return budgetHeadDepartmentName;
    }

    public String getBudgetHeadProjectCode() {
        return budgetHeadProjectCode;
    }

    public String getRequestorName() {
        return requestorName;
    }

    public String getRequestorDesignation() {
        return requestorDesignation;
    }

    public String getRequestorDepartment() {
        return requestorDepartment;
    }

    public String getRequestorMobile() {
        return requestorMobile;
    }

    public String getLogisticsName() {
        return logisticsName;
    }

    public String getLogisticsDesignation() {
        return logisticsDesignation;
    }

    public String getLogisticsMobile() {
        return logisticsMobile;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setArrivalAt(String arrivalAt) {
        this.arrivalAt = arrivalAt;
    }

    public void setDepartureAt(String departureAt) {
        this.departureAt = departureAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeInt(room);
        parcel.writeString(roomName);
        parcel.writeString(arrivalAt);
        parcel.writeString(departureAt);
        parcel.writeString(createdByName);
        parcel.writeString(createdAt);
        parcel.writeTypedList(editHistory);

        parcel.writeString(visitorName);
        parcel.writeString(visitorDesignation);
        parcel.writeString(visitorOrganisation);
        parcel.writeString(visitorGender);
        parcel.writeString(visitorAddress);
        parcel.writeString(visitorMobile);
        parcel.writeString(visitorEmail);
        parcel.writeString(purposeOfVisit);
        parcel.writeString(visitorCategory);

        parcel.writeByte((byte) (attenderRequired ? 1 : 0));
        parcel.writeInt(attenderCountPerDay);
        parcel.writeByte((byte) (attenderGeneralShift ? 1 : 0));
        parcel.writeByte((byte) (attenderMorningShift ? 1 : 0));
        parcel.writeByte((byte) (attenderDayShift ? 1 : 0));
        parcel.writeString(roomChargesStatus);
        parcel.writeString(attenderChargesStatus);
        parcel.writeString(roomChargesAmount);
        parcel.writeString(attenderChargesAmount);

        parcel.writeString(budgetHeadType);
        parcel.writeString(budgetHeadValue);
        parcel.writeString(budgetHeadName);
        parcel.writeString(budgetHeadDepartmentName);
        parcel.writeString(budgetHeadProjectCode);

        parcel.writeString(requestorName);
        parcel.writeString(requestorDesignation);
        parcel.writeString(requestorDepartment);
        parcel.writeString(requestorMobile);

        parcel.writeString(logisticsName);
        parcel.writeString(logisticsDesignation);
        parcel.writeString(logisticsMobile);

        parcel.writeString(status);
    }
}
