package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;

import java.util.ArrayList;
import java.util.List;

public final class RoomCache {

    private static List<RoomItem> cachedRooms = new ArrayList<>();

    private RoomCache() {
    }

    public static void setRooms(List<RoomItem> rooms) {
        cachedRooms = rooms != null ? new ArrayList<>(rooms) : new ArrayList<>();
    }

    public static List<RoomItem> getRooms() {
        return new ArrayList<>(cachedRooms);
    }

    public static boolean hasRooms() {
        return cachedRooms != null && !cachedRooms.isEmpty();
    }

    public static void clear() {
        if (cachedRooms != null) {
            cachedRooms.clear();
        }
    }
}