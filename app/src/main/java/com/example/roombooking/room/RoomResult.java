package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.utils.NullSafeCollections;

import java.util.ArrayList;
import java.util.List;

public class RoomResult {

    private final List<RoomItem> rooms;
    private final String errorMessage;
    private final boolean fromCache;

    public RoomResult(
            List<RoomItem> rooms,
            String errorMessage,
            boolean fromCache
    ) {
        this.rooms = rooms != null
                ? NullSafeCollections.copyWithoutNulls(rooms)
                : null;
        this.errorMessage = errorMessage;
        this.fromCache = fromCache;
    }

    public static RoomResult success(List<RoomItem> rooms, boolean fromCache) {
        return new RoomResult(rooms, null, fromCache);
    }

    public static RoomResult error(String errorMessage) {
        return new RoomResult(null, errorMessage, false);
    }

    public List<RoomItem> getRooms() {
        return rooms != null ? new ArrayList<>(rooms) : null;
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

    public boolean hasRooms() {
        return rooms != null && !rooms.isEmpty();
    }
}
