package com.example.roombooking.booking;

import com.google.gson.annotations.SerializedName;
import com.example.roombooking.utils.NullSafeCollections;

import java.util.List;

public class AvailableRoomsRangeResponse {

    @SerializedName("arrival_date")
    private String arrivalDate;

    @SerializedName("departure_date")
    private String departureDate;

    @SerializedName("prefix")
    private String prefix;

    @SerializedName("total_available_rooms")
    private int totalAvailableRooms;

    @SerializedName("rooms")
    private List<AvailableRoomItem> rooms;

    public String getArrivalDate() {
        return arrivalDate;
    }

    public String getDepartureDate() {
        return departureDate;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getTotalAvailableRooms() {
        return totalAvailableRooms;
    }

    public List<AvailableRoomItem> getRooms() {
        return rooms;
    }

    public boolean hasRooms() {
        return NullSafeCollections.hasNonNullItems(rooms);
    }
}
