package com.example.roombooking.model.room;

import com.example.roombooking.booking.AvailableRoomItem;
import com.example.roombooking.booking.RoomAvailabilityDay;
import com.example.roombooking.utils.NullSafeCollections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RoomInventory {

    private static final int DELTA_ROOM_COUNT = 8;
    private static final Set<String> DELTA_ROOM_NUMBERS = new HashSet<>(
            Arrays.asList("101A", "101B", "101C", "101D", "102A", "102B", "102C", "102D")
    );

    private RoomInventory() {
        // Utility class.
    }

    public static int displayTotalRooms(String prefix, int apiTotalRooms) {
        if (isDelta(prefix)) {
            return DELTA_ROOM_COUNT;
        }
        return Math.max(0, apiTotalRooms);
    }

    public static int displayAvailableRooms(String prefix, RoomAvailabilityDay day) {
        if (day == null) {
            return 0;
        }
        if (!isDelta(prefix)) {
            return Math.max(0, day.getAvailableRooms());
        }

        int bookedRooms = Math.max(0, day.getBookedRooms());
        return Math.max(0, Math.min(DELTA_ROOM_COUNT, DELTA_ROOM_COUNT - bookedRooms));
    }

    public static List<RoomItem> visibleRooms(List<RoomItem> rooms) {
        List<RoomItem> visibleRooms = new ArrayList<>();
        for (RoomItem room : NullSafeCollections.copyWithoutNulls(rooms)) {
            if (isVisibleRoom(room)) {
                visibleRooms.add(room);
            }
        }
        return visibleRooms;
    }

    public static List<AvailableRoomItem> visibleAvailableRooms(
            String selectedPrefix,
            List<AvailableRoomItem> rooms
    ) {
        List<AvailableRoomItem> visibleRooms = new ArrayList<>();
        for (AvailableRoomItem room : NullSafeCollections.copyWithoutNulls(rooms)) {
            if (isVisibleAvailableRoom(selectedPrefix, room)) {
                visibleRooms.add(room);
            }
        }
        return visibleRooms;
    }

    private static boolean isVisibleRoom(RoomItem room) {
        if (room == null) {
            return false;
        }
        if (!isDelta(room.getSafePrefix())) {
            return true;
        }
        return isAllowedDeltaRoom(room.getSafeNumber())
                || isAllowedDeltaRoom(room.getSafeSelectionLabel())
                || isAllowedDeltaRoom(room.getSafeRoomName());
    }

    private static boolean isVisibleAvailableRoom(
            String selectedPrefix,
            AvailableRoomItem room
    ) {
        if (room == null) {
            return false;
        }
        String prefix = firstNonBlank(room.getPrefix(), selectedPrefix);
        if (!isDelta(prefix)) {
            return true;
        }
        return isAllowedDeltaRoom(room.getSafeSelectionLabel())
                || isAllowedDeltaRoom(room.getSafeRoomName());
    }

    private static boolean isAllowedDeltaRoom(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String roomNumber : DELTA_ROOM_NUMBERS) {
            if (normalized.contains(roomNumber)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDelta(String prefix) {
        return RoomPrefix.DELTA.equalsIgnoreCase(safe(prefix));
    }

    private static String firstNonBlank(String first, String second) {
        String safeFirst = safe(first);
        return safeFirst.isEmpty() ? safe(second) : safeFirst;
    }

    private static String normalize(String value) {
        return safe(value)
                .toUpperCase(Locale.US)
                .replaceAll("[^A-Z0-9]", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
