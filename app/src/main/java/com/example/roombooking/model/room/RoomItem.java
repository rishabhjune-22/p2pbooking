package com.example.roombooking.model.room;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class RoomItem {

    @SerializedName("id")
    private int id;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("number")
    private String number;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("hostel_name")
    private String hostelName;

    @SerializedName("has_attached_bath")
    private boolean hasAttachedBath = true;

    @SerializedName("room_type")
    private String roomType;

    @SerializedName("selection_label")
    private String selectionLabel;

    @SerializedName("display_order")
    private int displayOrder;

    public RoomItem() {
        // Required for Gson/Retrofit.
    }

    public RoomItem(
            int id,
            String prefix,
            String number,
            String roomName
    ) {
        this.id = id;
        this.prefix = prefix;
        this.number = number;
        this.roomName = roomName;
    }

    public int getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNumber() {
        return number;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getHostelName() {
        return hostelName;
    }

    public boolean hasAttachedBath() {
        return hasAttachedBath;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getSelectionLabel() {
        return selectionLabel;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getSafePrefix() {
        return prefix != null ? prefix : "";
    }

    public String getSafeNumber() {
        return number != null ? number : "";
    }

    public String getSafeRoomName() {
        return roomName != null ? roomName : "";
    }

    public String getSafeHostelName() {
        return hostelName != null ? hostelName : "";
    }

    public String getSafeRoomType() {
        return roomType != null ? roomType : "room";
    }

    public String getSafeSelectionLabel() {
        return selectionLabel != null && !selectionLabel.trim().isEmpty()
                ? selectionLabel
                : getSafeNumber();
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setHostelName(String hostelName) {
        this.hostelName = hostelName;
    }

    public void setHasAttachedBath(boolean hasAttachedBath) {
        this.hasAttachedBath = hasAttachedBath;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setSelectionLabel(String selectionLabel) {
        this.selectionLabel = selectionLabel;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean hasSameContent(RoomItem other) {
        if (other == null) return false;

        return id == other.id
                && Objects.equals(prefix, other.prefix)
                && Objects.equals(number, other.number)
                && Objects.equals(roomName, other.roomName)
                && Objects.equals(hostelName, other.hostelName)
                && hasAttachedBath == other.hasAttachedBath
                && Objects.equals(roomType, other.roomType)
                && Objects.equals(selectionLabel, other.selectionLabel)
                && displayOrder == other.displayOrder;
    }

    @Override
    public String toString() {
        return getSafeRoomName();
    }
}
