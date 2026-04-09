package com.example.roombooking.model.booking;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class BookingItem implements Parcelable {

    @SerializedName("id")
    private int id;

    @SerializedName("room")
    private int room;

    @SerializedName("room_name")
    private String roomName;

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

    @SerializedName("arrival_date")
    private String arrivalDate;

    @SerializedName("arrival_time")
    private String arrivalTime;

    @SerializedName("departure_date")
    private String departureDate;

    @SerializedName("departure_time")
    private String departureTime;

    @SerializedName("purpose_of_visit")
    private String purposeOfVisit;

    @SerializedName("requestee_name")
    private String requesteeName;

    @SerializedName("requestee_designation")
    private String requesteeDesignation;

    @SerializedName("requestee_department")
    private String requesteeDepartment;

    @SerializedName("requestee_mobile")
    private String requesteeMobile;

    @SerializedName("logistics_name")
    private String logisticsName;

    @SerializedName("logistics_designation")
    private String logisticsDesignation;

    @SerializedName("logistics_mobile")
    private String logisticsMobile;

    @SerializedName("status")
    private String status;

    @SerializedName("created_by_username")
    private String createdByUsername;

    protected BookingItem(Parcel in) {
        id = in.readInt();
        room = in.readInt();
        roomName = in.readString();
        visitorName = in.readString();
        visitorDesignation = in.readString();
        visitorOrganisation = in.readString();
        visitorGender = in.readString();
        visitorAddress = in.readString();
        visitorMobile = in.readString();
        visitorEmail = in.readString();
        arrivalDate = in.readString();
        arrivalTime = in.readString();
        departureDate = in.readString();
        departureTime = in.readString();
        purposeOfVisit = in.readString();
        requesteeName = in.readString();
        requesteeDesignation = in.readString();
        requesteeDepartment = in.readString();
        requesteeMobile = in.readString();
        logisticsName = in.readString();
        logisticsDesignation = in.readString();
        logisticsMobile = in.readString();
        status = in.readString();
        createdByUsername = in.readString();
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

    public int getId() { return id; }
    public int getRoom() { return room; }
    public String getRoomName() { return roomName; }
    public String getVisitorName() { return visitorName; }
    public String getVisitorDesignation() { return visitorDesignation; }
    public String getVisitorOrganisation() { return visitorOrganisation; }
    public String getVisitorGender() { return visitorGender; }
    public String getVisitorAddress() { return visitorAddress; }
    public String getVisitorMobile() { return visitorMobile; }
    public String getVisitorEmail() { return visitorEmail; }
    public String getArrivalDate() { return arrivalDate; }
    public String getArrivalTime() { return arrivalTime; }
    public String getDepartureDate() { return departureDate; }
    public String getDepartureTime() { return departureTime; }
    public String getPurposeOfVisit() { return purposeOfVisit; }
    public String getRequesteeName() { return requesteeName; }
    public String getRequesteeDesignation() { return requesteeDesignation; }
    public String getRequesteeDepartment() { return requesteeDepartment; }
    public String getRequesteeMobile() { return requesteeMobile; }
    public String getLogisticsName() { return logisticsName; }
    public String getLogisticsDesignation() { return logisticsDesignation; }
    public String getLogisticsMobile() { return logisticsMobile; }
    public String getStatus() { return status; }
    public String getCreatedByUsername() { return createdByUsername; }


    public void setId(int id) {
        this.id = id;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public void setVisitorDesignation(String visitorDesignation) {
        this.visitorDesignation = visitorDesignation;
    }

    public void setVisitorOrganisation(String visitorOrganisation) {
        this.visitorOrganisation = visitorOrganisation;
    }

    public void setVisitorGender(String visitorGender) {
        this.visitorGender = visitorGender;
    }

    public void setVisitorAddress(String visitorAddress) {
        this.visitorAddress = visitorAddress;
    }

    public void setVisitorMobile(String visitorMobile) {
        this.visitorMobile = visitorMobile;
    }

    public void setVisitorEmail(String visitorEmail) {
        this.visitorEmail = visitorEmail;
    }

    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setDepartureDate(String departureDate) {
        this.departureDate = departureDate;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setPurposeOfVisit(String purposeOfVisit) {
        this.purposeOfVisit = purposeOfVisit;
    }

    public void setRequesteeName(String requesteeName) {
        this.requesteeName = requesteeName;
    }

    public void setRequesteeDesignation(String requesteeDesignation) {
        this.requesteeDesignation = requesteeDesignation;
    }

    public void setRequesteeDepartment(String requesteeDepartment) {
        this.requesteeDepartment = requesteeDepartment;
    }

    public void setRequesteeMobile(String requesteeMobile) {
        this.requesteeMobile = requesteeMobile;
    }

    public void setLogisticsName(String logisticsName) {
        this.logisticsName = logisticsName;
    }

    public void setLogisticsDesignation(String logisticsDesignation) {
        this.logisticsDesignation = logisticsDesignation;
    }

    public void setLogisticsMobile(String logisticsMobile) {
        this.logisticsMobile = logisticsMobile;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(room);
        parcel.writeString(roomName);
        parcel.writeString(visitorName);
        parcel.writeString(visitorDesignation);
        parcel.writeString(visitorOrganisation);
        parcel.writeString(visitorGender);
        parcel.writeString(visitorAddress);
        parcel.writeString(visitorMobile);
        parcel.writeString(visitorEmail);
        parcel.writeString(arrivalDate);
        parcel.writeString(arrivalTime);
        parcel.writeString(departureDate);
        parcel.writeString(departureTime);
        parcel.writeString(purposeOfVisit);
        parcel.writeString(requesteeName);
        parcel.writeString(requesteeDesignation);
        parcel.writeString(requesteeDepartment);
        parcel.writeString(requesteeMobile);
        parcel.writeString(logisticsName);
        parcel.writeString(logisticsDesignation);
        parcel.writeString(logisticsMobile);
        parcel.writeString(status);
        parcel.writeString(createdByUsername);
    }
}