package com.example.roombooking.room;

import com.example.roombooking.model.room.RoomItem;

import java.util.ArrayList;
import java.util.List;

public final class RoomMemoryCache {

    private static List<RoomItem> rooms = new ArrayList<>();

    private RoomMemoryCache() {
    }

    public static void setRooms(List<RoomItem> roomList) {
        rooms = roomList != null ? new ArrayList<>(roomList) : new ArrayList<>();
    }

    public static List<RoomItem> getRooms() {
        return new ArrayList<>(rooms);
    }

    public static boolean hasRooms() {
        return rooms != null && !rooms.isEmpty();
    }

    public static void clear() {
        rooms.clear();
    }
}