package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RoomMemoryCache {

    private static final Object LOCK = new Object();

    private static List<RoomItem> rooms = new ArrayList<>();

    private RoomMemoryCache() {
        // Utility class. No object required.
    }

    public static void setRooms(List<RoomItem> roomList) {
        synchronized (LOCK) {
            rooms = roomList != null
                    ? new ArrayList<>(roomList)
                    : new ArrayList<>();
        }
    }

    public static List<RoomItem> getRooms() {
        synchronized (LOCK) {
            return new ArrayList<>(rooms);
        }
    }

    public static boolean hasRooms() {
        synchronized (LOCK) {
            return rooms != null && !rooms.isEmpty();
        }
    }

    public static int size() {
        synchronized (LOCK) {
            return rooms != null ? rooms.size() : 0;
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            rooms.clear();
        }
    }

    public static List<RoomItem> emptyList() {
        return Collections.emptyList();
    }
}