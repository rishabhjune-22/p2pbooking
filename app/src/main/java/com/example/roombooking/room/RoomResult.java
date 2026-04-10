package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;

import java.util.List;

public class RoomResult {

    private final List<RoomItem> rooms;
    private final String errorMessage;
    private final boolean fromCache;

    public RoomResult(List<RoomItem> rooms, String errorMessage, boolean fromCache) {
        this.rooms = rooms;
        this.errorMessage = errorMessage;
        this.fromCache = fromCache;
    }

    public List<RoomItem> getRooms() {
        return rooms;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public boolean isSuccess() {
        return rooms != null;
    }
}