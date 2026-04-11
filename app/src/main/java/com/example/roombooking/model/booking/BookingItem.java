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

    @SerializedName("arrival_at")
    private String arrivalAt;

    @SerializedName("departure_at")
    private String departureAt;

    // Encryption fields (ONLY source of sensitive data)
    @SerializedName("encrypted_payload")
    private String encryptedPayload;

    @SerializedName("payload_nonce")
    private String payloadNonce;

    @SerializedName("payload_version")
    private int payloadVersion;

    @SerializedName("can_view_sensitive_details")
    private boolean canViewSensitiveDetails;

    // Visible to all users
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
        arrivalAt = in.readString();
        departureAt = in.readString();
        encryptedPayload = in.readString();
        payloadNonce = in.readString();
        payloadVersion = in.readInt();
        canViewSensitiveDetails = in.readByte() != 0;
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

    // Getters
    public int getId() { return id; }
    public int getRoom() { return room; }
    public String getRoomName() { return roomName; }

    public String getArrivalAt() { return arrivalAt; }
    public String getDepartureAt() { return departureAt; }

    public String getEncryptedPayload() { return encryptedPayload; }
    public String getPayloadNonce() { return payloadNonce; }
    public int getPayloadVersion() { return payloadVersion; }

    public boolean canDecrypt() { return canViewSensitiveDetails; }

    public String getRequesteeName() { return requesteeName; }
    public String getRequesteeDesignation() { return requesteeDesignation; }
    public String getRequesteeDepartment() { return requesteeDepartment; }
    public String getRequesteeMobile() { return requesteeMobile; }

    public String getLogisticsName() { return logisticsName; }
    public String getLogisticsDesignation() { return logisticsDesignation; }
    public String getLogisticsMobile() { return logisticsMobile; }

    public String getStatus() { return status; }
    public String getCreatedByUsername() { return createdByUsername; }

    // Utility
    public boolean hasEncryptedPayload() {
        return encryptedPayload != null && !encryptedPayload.trim().isEmpty()
                && payloadNonce != null && !payloadNonce.trim().isEmpty();
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
        parcel.writeString(encryptedPayload);
        parcel.writeString(payloadNonce);
        parcel.writeInt(payloadVersion);
        parcel.writeByte((byte) (canViewSensitiveDetails ? 1 : 0));
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






    public void setStatus(String status) { this.status = status; }
    public void setArrivalAt(String arrivalAt) { this.arrivalAt = arrivalAt; }
    public void setDepartureAt(String departureAt) { this.departureAt = departureAt; }
    public void setEncryptedPayload(String encryptedPayload) { this.encryptedPayload = encryptedPayload; }
    public void setPayloadNonce(String payloadNonce) { this.payloadNonce = payloadNonce; }
    public void setPayloadVersion(int payloadVersion) { this.payloadVersion = payloadVersion; }

}