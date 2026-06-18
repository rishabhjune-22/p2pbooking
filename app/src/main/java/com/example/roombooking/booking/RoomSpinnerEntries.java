package com.example.roombooking.booking;

import com.example.roombooking.model.room.RoomItem;
import com.example.roombooking.model.room.RoomPrefix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class RoomSpinnerEntries {

    private RoomSpinnerEntries() {
    }

    static List<RoomSpinnerEntry> build(List<RoomItem> rooms) {
        List<RoomSpinnerEntry> entries = new ArrayList<>();
        entries.add(RoomSpinnerEntry.prompt());

        for (String prefix : RoomPrefix.displayOrder()) {
            List<RoomItem> prefixRooms = roomsForPrefix(rooms, prefix);
            if (prefixRooms.isEmpty()) continue;

            RoomItem firstRoom = prefixRooms.get(0);
            entries.add(RoomSpinnerEntry.header(
                    prefix + " — " + firstRoom.getSafeHostelName()
            ));

            if (RoomPrefix.DELTA.equals(prefix)) {
                addSection(entries, prefixRooms, "chairman_flat", "Chairman Flat");
                addSection(entries, prefixRooms, "room", "Rooms");
            } else {
                addRooms(entries, prefixRooms, null);
            }
        }

        return entries;
    }

    private static List<RoomItem> roomsForPrefix(List<RoomItem> rooms, String prefix) {
        List<RoomItem> result = new ArrayList<>();
        for (RoomItem room : rooms) {
            if (room != null && prefix.equalsIgnoreCase(room.getSafePrefix())) {
                result.add(room);
            }
        }
        result.sort(Comparator.comparingInt(RoomItem::getDisplayOrder));
        return result;
    }

    private static void addSection(
            List<RoomSpinnerEntry> entries,
            List<RoomItem> rooms,
            String roomType,
            String heading
    ) {
        boolean hasRooms = false;
        for (RoomItem room : rooms) {
            if (roomType.equals(room.getSafeRoomType())) {
                hasRooms = true;
                break;
            }
        }
        if (!hasRooms) return;

        entries.add(RoomSpinnerEntry.header(heading));
        addRooms(entries, rooms, roomType);
    }

    private static void addRooms(
            List<RoomSpinnerEntry> entries,
            List<RoomItem> rooms,
            String requiredType
    ) {
        for (RoomItem room : rooms) {
            if (requiredType == null || requiredType.equals(room.getSafeRoomType())) {
                entries.add(RoomSpinnerEntry.room(room));
            }
        }
    }
}
